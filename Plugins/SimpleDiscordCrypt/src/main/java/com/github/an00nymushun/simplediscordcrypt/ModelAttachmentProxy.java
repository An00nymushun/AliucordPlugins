package com.github.an00nymushun.simplediscordcrypt;

import android.net.Uri;
import com.discord.models.message.Message;
import com.lytefast.flexinput.model.Attachment;

import java.lang.reflect.Field;

public class ModelAttachmentProxy {
    private static Field dataField;
    private static Field displayNameField;
    private static Field uriField;
    public static boolean setup;

    static {
        var modelAttachmentClass = Attachment.class;

        try {
            dataField = modelAttachmentClass.getDeclaredField("data");
            displayNameField = modelAttachmentClass.getDeclaredField("displayName");
            uriField = modelAttachmentClass.getDeclaredField("uri");

            dataField.setAccessible(true);
            displayNameField.setAccessible(true);
            uriField.setAccessible(true);

            setup = true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    static Object getData(Attachment modelAttachment) {
        try {
            return dataField.get(modelAttachment);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setData(Attachment modelAttachment, Object data) {
        try {
            dataField.set(modelAttachment, data);
        } catch (Exception ignored) {
        }
    }

    static String getDisplayName(Attachment modelAttachment) {
        try {
            return (String)displayNameField.get(modelAttachment);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setDisplayName(Attachment modelAttachment, String displayName) {
        try {
            displayNameField.set(modelAttachment, displayName);
        } catch (Exception ignored) {
        }
    }

    static Uri getUri(Attachment modelAttachment) {
        try {
            return (Uri)uriField.get(modelAttachment);
        } catch (Exception ignored) {
            return null;
        }
    }
    static void setUri(Attachment modelAttachment, Uri uri) {
        try {
            uriField.set(modelAttachment, uri);
        } catch (Exception ignored) {
        }
    }
}
