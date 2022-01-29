package com.github.an00nymushun.simplediscordcrypt;

import android.content.ContentResolver;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Utils {
    public static ContentResolver ContentResolver;
    public static SharedPreferences Settings;

    public static byte[] Concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] result = new byte[aLen + bLen];
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
    }

    public static byte[] ReadStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[0x100000];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public static byte[] PayloadDecode(String payload, int start, int length) {
        byte[] bytes = new byte[length];
        for(int i = 0; i < length; i++) {
            bytes[i] = (byte)(payload.charAt(start + i) - 0x2800);
        }
        return bytes;
    }
    public static String PayloadEncode(byte[] bytes) {
        int length = bytes.length;
        char[] chars = new char[length];
        for(int i = 0; i < length; i++) {
            chars[i] = (char)((bytes[i] & 0xFF) + 0x2800);
        }
        return new String(chars);
    }
    public static String PayloadEncode(byte[] bytesA, byte[] bytesB) {
        int aLen = bytesA.length;
        int bLen = bytesB.length;
        char[] chars = new char[aLen + bLen];
        for(int i = 0; i < aLen; i++) {
            chars[i] = (char)((bytesA[i] & 0xFF) + 0x2800);
        }
        for(int i = 0; i < bLen; i++) {
            chars[aLen + i] = (char)((bytesB[i] & 0xFF) + 0x2800);
        }
        return new String(chars);
    }

    public static byte[] AesDecrypt(byte[] key, byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key keyWrapper = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keyWrapper, new IvParameterSpec(Arrays.copyOf(bytes, 16)));
        byte[] decryptedBytes = cipher.doFinal(bytes, 16, bytes.length - 16);
        return decryptedBytes;
    }
    public static String AesDecryptString(byte[] key, byte[] bytes) throws Exception {
        return new String(AesDecrypt(key, bytes), StandardCharsets.UTF_8);
    }

    public static byte[] AesEncrypt(byte[] key, byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key keyWrapper = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keyWrapper);
        byte[] encryptedBytes = cipher.doFinal(bytes);
        return Concat(cipher.getIV(), encryptedBytes);
    }
    public static byte[] AesEncryptString(byte[] key, String string) throws Exception {
        return AesEncrypt(key, string.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] Sha512_128(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = new byte[16];
            System.arraycopy(md.digest(bytes), 0, hash, 0, 16);
            return hash;
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static byte[] Sha512_128(byte[] bytesA, byte[] bytesB) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(bytesA);
            byte[] hash = new byte[16];
            System.arraycopy(md.digest(bytesB), 0, hash, 0, 16);
            return hash;
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
