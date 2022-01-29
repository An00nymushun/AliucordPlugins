package com.github.an00nymushun.simplediscordcrypt;

import com.discord.api.message.Message;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.embed.MessageEmbed;

import java.lang.reflect.Field;
import java.util.List;

public class MessageProxy {
    private static Field contentField;
    private static Field attachmentsField;
    private static Field embedsField;
    public static boolean setup;

    static {
        var messageClass = Message.class;

        try {
            contentField = messageClass.getDeclaredField("content");
            attachmentsField = messageClass.getDeclaredField("attachments");
            embedsField = messageClass.getDeclaredField("embeds");

            contentField.setAccessible(true);
            attachmentsField.setAccessible(true);
            embedsField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static String getContent(Message message) {
        try {
            return (String)contentField.get(message);
        } catch(Exception ignored) {
            return null;
        }
    }
    static void setContent(Message message, String content) {
        try {
            contentField.set(message, content);
        } catch(Exception ignored) {
        }
    }

    static List<MessageAttachment> getAttachments(Message message) {
        try {
            return (List<MessageAttachment>)attachmentsField.get(message);
        } catch(Exception ignored) {
            return null;
        }
    }
    static void setAttachments(Message message, List<MessageAttachment> attachments) {
        try {
            attachmentsField.set(message, attachments);
        } catch(Exception ignored) {
        }
    }

    static List<MessageEmbed> getEmbeds(Message message) {
        try {
            return (List<MessageEmbed>)embedsField.get(message);
        } catch(Exception ignored) {
            return null;
        }
    }
    static void setEmbeds(Message message, List<MessageEmbed> embeds) {
        try {
            embedsField.set(message, embeds);
        } catch(Exception ignored) {
        }
    }
}

