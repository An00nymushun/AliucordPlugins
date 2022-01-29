package com.github.an00nymushun.simplediscordcrypt;

import com.discord.api.message.Message;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.embed.MessageEmbed;

import java.lang.reflect.Field;
import java.util.List;

public class AttachmentProxy {
    private static Field filenameField;
    private static Field urlField;
    private static Field proxyUrlField;
    private static Field heightField;
    private static Field widthField;
    private static Field sizeField;
    public static boolean setup;

    static {
        var attachmentClass = MessageAttachment.class;

        try {
            filenameField = attachmentClass.getDeclaredField("filename");
            urlField = attachmentClass.getDeclaredField("url");
            proxyUrlField = attachmentClass.getDeclaredField("proxyUrl");
            heightField = attachmentClass.getDeclaredField("height");
            widthField = attachmentClass.getDeclaredField("width");
            sizeField = attachmentClass.getDeclaredField("size");

            filenameField.setAccessible(true);
            urlField.setAccessible(true);
            proxyUrlField.setAccessible(true);
            heightField.setAccessible(true);
            widthField.setAccessible(true);
            sizeField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static String getFilename(MessageAttachment message) {
        try {
            return (String)filenameField.get(message);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setFilename(MessageAttachment message, String filename) {
        try {
            filenameField.set(message, filename);
        } catch (Exception ignored) {
        }
    }

    static String getUrl(MessageAttachment message) {
        try {
            return (String)urlField.get(message);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setUrl(MessageAttachment message, String url) {
        try {
            urlField.set(message, url);
        } catch (Exception ignored) {
        }
    }

    static String getProxyUrl(MessageAttachment message) {
        try {
            return (String)proxyUrlField.get(message);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setProxyUrl(MessageAttachment message, String proxyUrl) {
        try {
            proxyUrlField.set(message, proxyUrl);
        } catch (Exception ignored) {
        }
    }

    static Integer getHeight(MessageAttachment message) {
        try {
            return (Integer)heightField.get(message);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setHeight(MessageAttachment message, Integer height) {
        try {
            heightField.set(message, height);
        } catch (Exception ignored) {
        }
    }

    static Integer getWidth(MessageAttachment message) {
        try {
            return (Integer)widthField.get(message);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setWidth(MessageAttachment message, Integer width) {
        try {
            widthField.set(message, width);
        } catch (Exception ignored) {
        }
    }

    static long getSize(MessageAttachment message) {
        try {
            return sizeField.getLong(message);
        } catch (Exception ignored) {
            return 0;
        }
    }
    static void setSize(MessageAttachment message, long size) {
        try {
            sizeField.setLong(message, size);
        } catch (Exception ignored) {
        }
    }
}
