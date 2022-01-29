package com.github.an00nymushun.simplediscordcrypt;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public final class Database {
    private static class HashContainer {
        public byte[] Bytes;

        public HashContainer(byte[] bytes) {
            this.Bytes = bytes;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.Bytes);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (obj instanceof HashContainer) && Arrays.equals(this.Bytes, ((HashContainer)obj).Bytes);
        }
    }
    public static class Channel {
        public boolean Encrypted;
        public byte[] KeyHash;
        public byte[] KeyBytes;
        public Channel(boolean encrypted, byte[] keyHash, byte[] keyBytes) {
            this.Encrypted = encrypted;
            this.KeyHash = keyHash;
            this.KeyBytes = keyBytes;
        }
    }

    private HashMap<HashContainer, byte[]> keys;
    private HashMap<Long, Channel> channels;
    private byte[] latestKeyHash;
    private byte[] latestKeyBytes;
    private long latestChannelId;
    private Channel latestChannelConfig;

    public static @NonNull Database New() {
        Database database = new Database();
        database.keys = new HashMap<>();
        database.channels = new HashMap<>();
        return database;
    }
    public static @Nullable Database Load(Uri fileUri, String fileName) {
        Database database = new Database();
        database.keys = new HashMap<>();
        database.channels = new HashMap<>();
        return database.importFile(fileUri, fileName) ? database : null;
    }
    public static @Nullable Database Load(String dbPath) {
        if(dbPath != null) {
            Uri databaseUri = Uri.parse(dbPath);
            return Load(databaseUri, databaseUri.getLastPathSegment());
        }
        return null;
    }

    public @Nullable byte[] GetKey(byte[] keyHash) { //assuming that keyHash won't change
        if(Arrays.equals(keyHash, latestKeyHash)) return latestKeyBytes;
        byte[] result = keys.get(new HashContainer(keyHash));
        latestKeyHash = keyHash;
        latestKeyBytes = result;
        return result;
    }
    public @Nullable Channel GetChannelConfig(long channelId) {
        if(channelId == latestChannelId) return latestChannelConfig;
        Channel result = channels.get(channelId);
        latestChannelId = channelId;
        latestChannelConfig = result;
        return result;
    }

    private void importKeys(JsonReader jsonReader) throws IOException {
        HashMap<HashContainer, byte[]> keys = this.keys;
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String keyHashBase64 = jsonReader.nextName();
            byte[] keyHash = Base64.decode(keyHashBase64, Base64.DEFAULT);
            byte[] keyBytes = null;
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                char field = jsonReader.nextName().charAt(0);
                switch(field) {
                    case 'k'/*key*/:
                        keyBytes = Base64.decode(jsonReader.nextString(), Base64.DEFAULT);
                        break;
                    default:
                        jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            keys.put(new HashContainer(keyHash), keyBytes);
        }
        jsonReader.endObject();
    }
    private ArrayList<Pair<Long, Channel>> readChannels(JsonReader jsonReader) throws IOException {
        ArrayList<Pair<Long, Channel>> channelConfigList = new ArrayList<>();
        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            long channelId = Long.parseLong(jsonReader.nextName()); //TODO parseUnsignedLong when it's common
            byte[] keyHashBytes = null;
            boolean encrypted = false;
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                char field = jsonReader.nextName().charAt(0);
                switch(field) {
                    case 'k'/*keyHash*/:
                        keyHashBytes = Base64.decode(jsonReader.nextString(), Base64.DEFAULT);
                        break;
                    case 'e'/*encrypted*/:
                        JsonToken valueType = jsonReader.peek();
                        if(valueType == JsonToken.NUMBER) {
                            encrypted = (jsonReader.nextInt() != 0);
                        }
                        else if(valueType == JsonToken.BOOLEAN) {
                            encrypted = jsonReader.nextBoolean();
                        }
                        else jsonReader.skipValue();
                        break;
                    default:
                        jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if(!encrypted) continue; //TODO implement channel settings
            channelConfigList.add(new Pair<>(channelId, new Channel(encrypted, keyHashBytes, null)));
        }
        jsonReader.endObject();
        return channelConfigList;
    }
    private boolean importChannels(ArrayList<Pair<Long, Channel>> channelConfigList) {
        if(channelConfigList == null) return false;
        boolean noErrors = true;
        HashMap<HashContainer, byte[]> keys = this.keys;
        HashMap<Long, Channel> channels = this.channels;

        for(Pair<Long, Channel> channelEntry : channelConfigList) {
            Channel channelConfig = channelEntry.second;
            byte[] channelKeyBytes = keys.get(new HashContainer(channelConfig.KeyHash));
            if(channelKeyBytes != null) {
                channelConfig.KeyBytes = channelKeyBytes;
                channels.put(channelEntry.first, channelConfig);
            }
            else {
                noErrors = false;
            }
        }

        return noErrors;
    }
    private boolean importFile(Uri fileUri, String fileName) {
        ArrayList<Pair<Long, Channel>> channelConfigList = null;
        try {
            if(fileName.endsWith(".dat")) return false; //TODO
            InputStream stream = Utils.ContentResolver.openInputStream(fileUri);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("keys")) importKeys(jsonReader);
                else if(name.equals("channels")) channelConfigList = readChannels(jsonReader);
                else if(name.equals("isEncrypted")) { if(jsonReader.nextBoolean()) return false; } //TODO
                else jsonReader.skipValue();
            }
        }
        catch(Exception e) { return false; }

        importChannels(channelConfigList);

        return true;
    }
}
