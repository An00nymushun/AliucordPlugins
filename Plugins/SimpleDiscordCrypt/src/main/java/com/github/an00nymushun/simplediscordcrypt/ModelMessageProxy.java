package com.github.an00nymushun.simplediscordcrypt;

import com.discord.models.message.Message;

import java.lang.reflect.Field;
import java.util.List;

public class ModelMessageProxy {
    private static Field contentField;
    public static boolean setup;

    static {
        var modelMessageClass = Message.class;

        try {
            contentField = modelMessageClass.getDeclaredField("content");

            contentField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static String getContent(Message modelMessage) {
        try {
            return (String)contentField.get(modelMessage);
        } catch (Exception ignored) {
            return null;
        }
    }

    static void setContent(Message modelMessage, String content) {
        try {
            contentField.set(modelMessage, content);
        } catch (Exception ignored) {
        }
    }
}

