package com.github.an00nymushun.simplediscordcrypt;

import com.discord.api.message.embed.EmbedAuthor;
import com.discord.api.message.embed.MessageEmbed;

import java.lang.reflect.Field;

public class EmbedAuthorProxy {
    private static Field nameField;
    public static boolean setup;

    static {
        var embedAuthorClass = EmbedAuthor.class;

        try {
            nameField = embedAuthorClass.getDeclaredField("name");

            nameField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static String getName(EmbedAuthor embedAuthor) {
        try {
            return (String)nameField.get(embedAuthor);
        } catch (Exception ignored) {
            return null;
        }
    }
}

