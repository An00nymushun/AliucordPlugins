package com.github.an00nymushun.simplediscordcrypt;

import com.discord.api.message.Message;
import com.discord.api.message.attachment.MessageAttachment;
import com.discord.api.message.embed.EmbedAuthor;
import com.discord.api.message.embed.EmbedField;
import com.discord.api.message.embed.EmbedFooter;
import com.discord.api.message.embed.MessageEmbed;

import java.lang.reflect.Field;
import java.util.List;

public class EmbedProxy {
    private static Field authorField;
    private static Field descriptionField;
    private static Field footerField;
    public static boolean setup;

    static {
        var embedClass = MessageEmbed.class;

        try {
            authorField = embedClass.getDeclaredField("author");
            descriptionField = embedClass.getDeclaredField("description");
            footerField = embedClass.getDeclaredField("footer");

            authorField.setAccessible(true);
            descriptionField.setAccessible(true);
            footerField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static EmbedAuthor getAuthor(MessageEmbed embed) {
        try {
            return (EmbedAuthor)authorField.get(embed);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setAuthor(MessageEmbed embed, EmbedAuthor author) {
        try {
            authorField.set(embed, author);
        } catch (Exception ignored) {
        }
    }

    static String getDescription(MessageEmbed embed) {
        try {
            return (String)descriptionField.get(embed);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setDescription(MessageEmbed embed, String description) {
        try {
            descriptionField.set(embed, description);
        } catch (Exception ignored) {
        }
    }

    static EmbedFooter getFooter(MessageEmbed embed) {
        try {
            return (EmbedFooter)footerField.get(embed);
        } catch (Exception ignored) {
            return null;
        }
    }
}

