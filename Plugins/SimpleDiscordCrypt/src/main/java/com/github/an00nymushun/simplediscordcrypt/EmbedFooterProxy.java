package com.github.an00nymushun.simplediscordcrypt;

import com.discord.api.message.embed.EmbedAuthor;
import com.discord.api.message.embed.EmbedFooter;

import java.lang.reflect.Field;

public class EmbedFooterProxy {
    private static Field textField;
    public static boolean setup;

    static {
        var embedFooterClass = EmbedFooter.class;

        try {
            textField = embedFooterClass.getDeclaredField("text");

            textField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static String getText(EmbedFooter embedFooter) {
        try {
            return (String)textField.get(embedFooter);
        } catch (Exception ignored) {
            return null;
        }
    }
}
