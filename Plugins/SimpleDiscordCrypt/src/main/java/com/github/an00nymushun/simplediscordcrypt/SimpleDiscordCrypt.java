package com.github.an00nymushun.simplediscordcrypt;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.transition.Visibility;
import com.aliucord.CollectionUtils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.wrappers.embeds.MessageEmbedWrapper;

import android.net.Uri;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.discord.api.message.allowedmentions.MessageAllowedMentions;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.embed.EmbedAuthor;
import com.discord.api.message.embed.EmbedFooter;
import com.discord.api.message.embed.MessageEmbed;
import com.discord.stores.StoreMessagesLoader;
import com.discord.utilities.messagesend.MessageRequest;
import com.lytefast.flexinput.model.Attachment;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreUserTyping;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;
import com.discord.api.message.Message;
import com.discord.stores.StoreMessages;

// Aliucord Plugin annotation. Must be present on the main class of your plugin
@AliucordPlugin(requiresRestart = false /* Whether your plugin requires a restart after being installed/updated */)
// Plugin class. Must extend Plugin and override start and stop
// Learn more: https://github.com/Aliucord/documentation/blob/main/plugin-dev/1_introduction.md#basic-plugin-structure
public class SimpleDiscordCrypt extends Plugin {
    @Override
    public void start(Context context) throws Throwable {

        if( !AttachmentProxy.setup ||
            !EmbedAuthorProxy.setup ||
            !EmbedFooterProxy.setup ||
            !EmbedProxy.setup ||
            !MessageProxy.setup ||
            !ModelAttachmentProxy.setup ||
            !ModelMessageProxy.setup)
            throw new Exception("Plugin seems to be outdated.");

        Utils.ContentResolver = context.getContentResolver();
        SharedPreferences settings = context.getSharedPreferences("SimpleDiscordCrypt", Context.MODE_PRIVATE);
        Utils.Settings = settings;
        Database database = Database.Load(settings.getString("DbPath", null));
        if(database == null) database = Database.New();
        Db = database;
        proxyUrl = settings.getString("ProxyUrl", null);
        encryptedMessageRegex = Pattern.compile("^([⠀-⣿]{16,}) `(?:SimpleDiscordCrypt|\uD835\uDE1A\uD835\uDE2A\uD835\uDE2E\uD835\uDE31\uD835\uDE2D\uD835\uDE26\uD835\uDE0B\uD835\uDE2A\uD835\uDE34\uD835\uDE24\uD835\uDE30\uD835\uDE33\uD835\uDE25\uD835\uDE0A\uD835\uDE33\uD835\uDE3A\uD835\uDE31\uD835\uDE35)`$");
        payloadRegex = Pattern.compile("^[⠀-⣿]{16,}$");
        triggerRegex = Pattern.compile("^(:?NOENC:?|<:NOENC:\\d{1,20}>)?(:?ENC(?::|\\b)|<:ENC:\\d{1,20}>)?\\s*(.*)", Pattern.DOTALL);
        cacheDir = context.getCacheDir() + File.separator + "SimpleDiscordCrypt" + File.separator;
        proxyRegex = Pattern.compile("^PROXY\\s*(.*?)/?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        File directory = new File(cacheDir);
        if(!directory.exists()) directory.mkdir();
        directory.deleteOnExit();

        patcher.patch(
                StoreMessages.class.getDeclaredMethod("handleMessageCreate", List.class),
                new PreHook(hookParam -> {
                    List<Message> messageList = (List<Message>)hookParam.args[0];
                    for(Message message : messageList) {
                        processApiMessage(message);
                    }
                })
        );

        patcher.patch(
                StoreMessages.class.getDeclaredMethod("handleMessageUpdate", Message.class),
                new PreHook(hookParam -> {
                    Message message = (Message)hookParam.args[0];
                    processApiMessage(message);
                })
        );

        patcher.patch(
                StoreMessages.class.getDeclaredMethod("handleMessagesLoaded", StoreMessagesLoader.ChannelChunk.class),
                new PreHook(hookParam -> {
                    StoreMessagesLoader.ChannelChunk chunk = (StoreMessagesLoader.ChannelChunk)hookParam.args[0];
                    List<com.discord.models.message.Message> messages = chunk.getMessages();
                    for (var message : messages) {
                        processModelMessage(message);
                    }
                })
        );

        patcher.patch(
                MessageRequest.Send.class.getDeclaredConstructor(
                        com.discord.models.message.Message.class,
                        com.discord.api.activity.Activity.class,
                        List.class,
                        kotlin.jvm.functions.Function2.class,
                        kotlin.jvm.functions.Function1.class,
                        kotlin.jvm.functions.Function1.class,
                        long.class
                ),
                new PreHook(hookParam -> {
                    Object[] args = hookParam.args;

                    var message = (com.discord.models.message.Message)args[0];

                    List<Attachment> attachments = (List)args[2];
                    if(!checkDatabaseImport(message, attachments)) {
                        Database.Channel channelConfig = Db.GetChannelConfig(message.getChannelId());
                        Matcher prefixMatch = triggerRegex.matcher(message.getContent());
                        prefixMatch.find();
                        String content = prefixMatch.group(3);

                        boolean forceUnenc = (prefixMatch.group(1) != null);
                        boolean forceEnc = (prefixMatch.group(2) != null);
                        if(forceEnc || forceUnenc) {
                            ModelMessageProxy.setContent(message, content);

                            if(forceEnc) {
                                Matcher proxyMatch = proxyRegex.matcher(content);
                                if(proxyMatch.find()) {
                                    proxyUrl = proxyMatch.group(1);
                                    Utils.Settings.edit().putString("ProxyUrl", proxyUrl).apply();
                                    ModelMessageProxy.setContent(message, "File proxy set!");
                                }
                            }
                        }

                        if(channelConfig != null && !forceUnenc && (channelConfig.Encrypted || forceEnc)) {
                            encryptMessage(channelConfig.KeyHash, channelConfig.KeyBytes, message, attachments);
                        }
                    }
                })
        );

        patcher.patch(
                MessageRequest.Edit.class.getDeclaredConstructor(
                        long.class,
                        String.class,
                        long.class,
                        MessageAllowedMentions.class,
                        long.class
                ),
                new PreHook(hookParam -> {
                    Object[] args = hookParam.args;

                    long channelId = (long)args[1];
                    String content = (String)args[2];
                    Database.Channel channelConfig = Db.GetChannelConfig(channelId);
                    Matcher prefixMatch = triggerRegex.matcher(content);
                    prefixMatch.find();
                    content = prefixMatch.group(3);
                    boolean forceUnenc = (prefixMatch.group(1) != null);
                    boolean forceEnc = (prefixMatch.group(2) != null);
                    if(channelConfig != null && !forceUnenc && (forceEnc || channelConfig.Encrypted)) {
                        content = encryptContent(channelConfig.KeyHash, channelConfig.KeyBytes, content);
                    }
                    args[2] = content;
                })
        );
    }

    @Override
    public void stop(Context context) {
        // Remove all patches
        patcher.unpatchAll();
    }

    public static Database Db;
    private static Pattern encryptedMessageRegex;
    private static Pattern payloadRegex;
    private static Pattern triggerRegex;
    private static String cacheDir;
    private static String proxyUrl;
    private static Pattern proxyRegex;


    private final static int Base64EncodeFlags = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    private static boolean checkDatabaseImport(com.discord.models.message.Message message, List<Attachment> attachments) {
        if(attachments.size() == 1) {
            Attachment attachment = attachments.get(0);
            Uri fileUri = attachment.getUri();
            String attachmentFilename = fileUri.getLastPathSegment();
            if(attachmentFilename != null && attachmentFilename.contains("SimpleDiscordCrypt") &&
                    (attachmentFilename.endsWith(".json") || attachmentFilename.endsWith(".dat"))) {
                attachments.clear();
                Database database = Database.Load(fileUri, attachmentFilename);
                boolean success = (database != null);
                if(success) {
                    Db = database;
                    Utils.Settings.edit().putString("DbPath", fileUri.toString()).apply();
                }
                ModelMessageProxy.setContent(message, "Database import " + (success ? "success" : "failed"));
                return true;
            }
        }
        return false;
    }

    private static String encryptContent(byte[] keyHash, byte[] key, String content) {
        try {
            content = Utils.PayloadEncode(keyHash, Utils.AesEncryptString(key, content)) + " `SimpleDiscordCrypt`";
        }
        catch(Exception e) {
            content = content.replaceAll("[^\\s]", "X") + "\nMessage failed to encrypt";
        }
        return content;
    }
    private static void encryptAttachments(com.discord.models.message.Message message, byte[] key, List<Attachment> attachments) {
        try {
            for (Attachment attachment : attachments) {
                ModelAttachmentProxy.setData(attachment, null);
                String filename = attachment.getDisplayName();
                int dotIndex = filename.lastIndexOf('.');
                int filenameLength = filename.length();
                if(filenameLength > 47) {
                    if (dotIndex == -1) {
                        filename = filename.substring(0, 47);
                    } else {
                        String extension = filename.substring(dotIndex);
                        filename = filename.substring(0, 47 - extension.length()) + extension;
                    }
                }
                byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
                if(filenameBytes.length > 47) {
                    String extension = dotIndex != -1 ? filename.substring(dotIndex) : "";
                    filenameBytes = ("file" + extension).getBytes(StandardCharsets.UTF_8);
                }
                String encryptedFilename;
                do {
                    encryptedFilename = Base64.encodeToString(Utils.AesEncrypt(key, filenameBytes), Base64EncodeFlags);
                } while (encryptedFilename.startsWith("_") || encryptedFilename.endsWith("_"));

                ModelAttachmentProxy.setDisplayName(attachment, encryptedFilename);
                Uri uri = null;

                InputStream fileStream = Utils.ContentResolver.openInputStream(attachment.getUri());
                if(fileStream != null) {
                    byte[] fileData = Utils.ReadStream(fileStream);
                    fileData = Utils.AesEncrypt(key, fileData);
                    File cacheFile = new File(cacheDir + encryptedFilename);
                    cacheFile.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
                    fileOutputStream.write(fileData);
                    fileOutputStream.close();
                    uri = Uri.fromFile(cacheFile);
                }

                ModelAttachmentProxy.setUri(attachment, uri);
            }
        }
        catch (Exception ignored) {
        }
    }
    private static void encryptMessage(byte[] keyHash, byte[] key, com.discord.models.message.Message message, List<Attachment> attachments) {
        ModelMessageProxy.setContent(message, encryptContent(keyHash, key, ModelMessageProxy.getContent(message)));

        if(attachments.size() != 0) encryptAttachments(message, key, attachments);
    }

    private static void decryptApiMessage(Message message, String payload)
    {
        List<MessageAttachment> attachments = MessageProxy.getAttachments(message);
        List<MessageEmbed> embeds = MessageProxy.getEmbeds(message);

        String newContent = decryptMessage(attachments, embeds, payload);

        MessageProxy.setContent(message, newContent);
    }

    private static void decryptModelMessage(com.discord.models.message.Message message, String payload)
    {
        List<MessageAttachment> attachments = message.getAttachments();
        List<MessageEmbed> embeds = message.getEmbeds();

        String newContent = decryptMessage(attachments, embeds, payload);

        ModelMessageProxy.setContent(message, newContent);
    }

    private static String decryptMessage(List<MessageAttachment> attachments, List<MessageEmbed> embeds, String payload) {
        if(embeds != null && embeds.size() != 0) embeds.clear();
        byte[] keyHashBytes = Utils.PayloadDecode(payload, 0, 16);
        byte[] keyBytes = Db.GetKey(keyHashBytes);
        boolean failed = false;
        String newContent;
        if(keyBytes != null) {
            int payloadLength = payload.length();
            if(payloadLength == 16) {
                newContent = "<:ENC:465534298662109185>\u2063";
            }
            else {
                byte[] encryptedBytes = Utils.PayloadDecode(payload, 16, payloadLength - 16);
                try {
                    newContent = "<:ENC:465534298662109185>" + Utils.AesDecryptString(keyBytes, encryptedBytes);
                }
                catch(Exception e) {
                    newContent = "```diff\n-\u2063----ENCRYPTED MESSAGE WITH UNKNOWN FORMAT-----\n```";
                    failed = true;
                }
            }
        }
        else {
            newContent = "```fix\n-----ENCRYPTED MESSAGE WITH UNKNOWN KEY-----\n```";
            failed = true;
        }

        if(attachments != null) {
            if (!failed) {
                String proxyUrl = SimpleDiscordCrypt.proxyUrl;
                if (proxyUrl != null) {
                    for (MessageAttachment attachment : attachments) {
                        String decryptedFilename;
                        try {
                            decryptedFilename = Utils.AesDecryptString(keyBytes, Base64.decode(AttachmentProxy.getFilename(attachment), Base64.URL_SAFE));
                        } catch (Exception e) {
                            decryptedFilename = "file";
                        }
                        String originalUrl = AttachmentProxy.getUrl(attachment);
                        int attachmentIdEnd = originalUrl.lastIndexOf('/');
                        int attachmentIdStart = originalUrl.lastIndexOf('/', attachmentIdEnd - 1);
                        int index = originalUrl.lastIndexOf('/', attachmentIdStart - 1);
                        AttachmentProxy.setFilename(attachment, decryptedFilename);
                        AttachmentProxy.setWidth(attachment, 500);
                        AttachmentProxy.setHeight(attachment, 500);
                        String keyHash = Base64.encodeToString(keyHashBytes, Base64EncodeFlags);
                        String attachmentId = originalUrl.substring(attachmentIdStart + 1, attachmentIdEnd);
                        String proof = Base64.encodeToString(Utils.Sha512_128(keyBytes, attachmentId.getBytes(StandardCharsets.UTF_8)), Base64EncodeFlags);
                        String attachmentPath = originalUrl.substring(index);
                        String proxyParams = keyHash + '/' + proof + attachmentPath + '/' + decryptedFilename;
                        AttachmentProxy.setUrl(attachment, proxyUrl + "/file/" + proxyParams);
                        AttachmentProxy.setProxyUrl(attachment, proxyUrl + "/thumb/" + proxyParams);
                    }
                } else {
                    if (attachments.size() != 0) {
                        for (MessageAttachment attachment : attachments) {
                            String decryptedFilename;
                            try {
                                decryptedFilename = Utils.AesDecryptString(keyBytes, Base64.decode(AttachmentProxy.getFilename(attachment), Base64.URL_SAFE));
                            } catch (Exception e) {
                                decryptedFilename = "file";
                            }
                            newContent += "\nAttachment: " + decryptedFilename;
                        }
                    }
                    attachments.clear();
                }
            } else {
                if (attachments.size() != 0) attachments.clear();
            }
        }

        return newContent;
    }

    private static void processApiEmbed(Message message, MessageEmbed embed) {
        EmbedAuthor author = EmbedProxy.getAuthor(embed);
        if(author != null) {
            String messageType = EmbedAuthorProxy.getName(author);
            if("-----ENCRYPTED MESSAGE-----".equals(messageType)) {
                String description = EmbedProxy.getDescription(embed);
                if(description != null) {
                    Matcher payloadMatch = payloadRegex.matcher(description);
                    if(payloadMatch.find()) {
                        decryptApiMessage(message, payloadMatch.group(0));
                        return;
                    }
                }
            }
            else if("-----SYSTEM MESSAGE-----".equals(messageType)) {
                MessageProxy.setContent(message, "-----SYSTEM MESSAGE-----"); //TODO
                return;
            }
        }
        MessageProxy.setContent(message, "```diff\n-\u2063----ENCRYPTED MESSAGE WITH UNKNOWN FORMAT-----\n```");
    }

    private static void processModelEmbed(com.discord.models.message.Message message, MessageEmbed embed) {
        EmbedAuthor author = EmbedProxy.getAuthor(embed);
        if(author != null) {
            String messageType = EmbedAuthorProxy.getName(author);
            if("-----ENCRYPTED MESSAGE-----".equals(messageType)) {
                String description = EmbedProxy.getDescription(embed);
                if(description != null) {
                    Matcher payloadMatch = payloadRegex.matcher(description);
                    if(payloadMatch.find()) {
                        decryptModelMessage(message, payloadMatch.group(0));
                        return;
                    }
                }
            }
            else if("-----SYSTEM MESSAGE-----".equals(messageType)) {
                ModelMessageProxy.setContent(message, "-----SYSTEM MESSAGE-----"); //TODO
                return;
            }
        }
        ModelMessageProxy.setContent(message, "```diff\n-\u2063----ENCRYPTED MESSAGE WITH UNKNOWN FORMAT-----\n```");
    }

    private static void processApiMessage(Message message) {
        //if(message.isLocal()) return;

        String content = MessageProxy.getContent(message);

        if(content != null && content.length() >= 37) { //min length of an encrypted message (16 char key hash + space + `SimpleDiscordCrypt`)
            Matcher encryptedMessageMatch = encryptedMessageRegex.matcher(content);
            if (encryptedMessageMatch.find()) {
                String payload = encryptedMessageMatch.group(1);
                decryptApiMessage(message, payload);
            }
        }
        else {
            List<MessageEmbed> embeds = MessageProxy.getEmbeds(message);
            if(embeds != null && embeds.size() == 1) {
                MessageEmbed embed = embeds.get(0);
                EmbedFooter footer = EmbedProxy.getFooter(embed);
                if(footer != null) {
                    String footerText = EmbedFooterProxy.getText(footer);
                    if("SimpleDiscordCrypt".equals(footerText) || "𝘚𝘪𝘮𝘱𝘭𝘦𝘋𝘪𝘴𝘤𝘰𝘳𝘥𝘊𝘳𝘺𝘱𝘵".equals(footerText)) {
                        embeds.clear();
                        processApiEmbed(message, embed);
                    }
                }
            }
        }
    }

    private static void processModelMessage(com.discord.models.message.Message message) {
        if(message.isLocal()) return;

        String content = message.getContent();

        if(content != null && content.length() >= 37) { //min length of an encrypted message (16 char key hash + space + `SimpleDiscordCrypt`)
            Matcher encryptedMessageMatch = encryptedMessageRegex.matcher(content);
            if (encryptedMessageMatch.find()) {
                String payload = encryptedMessageMatch.group(1);
                decryptModelMessage(message, payload);
            }
        }
        else {
            List<MessageEmbed> embeds = message.getEmbeds();
            if(embeds != null && embeds.size() == 1) {
                MessageEmbed embed = embeds.get(0);
                EmbedFooter footer = EmbedProxy.getFooter(embed);
                if(footer != null) {
                    String footerText = EmbedFooterProxy.getText(footer);
                    if("SimpleDiscordCrypt".equals(footerText) || "𝘚𝘪𝘮𝘱𝘭𝘦𝘋𝘪𝘴𝘤𝘰𝘳𝘥𝘊𝘳𝘺𝘱𝘵".equals(footerText)) {
                        embeds.clear();
                        processModelEmbed(message, embed);
                    }
                }
            }
        }
    }
}
