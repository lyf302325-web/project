package com.attendance.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String md5(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encode(String password, String salt) {
        if (salt == null) {
            salt = "";
        }
        return md5(password + salt);
    }

    public static boolean matches(String rawPassword, String salt, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String rawEncoded = encode(rawPassword, salt);
        return rawEncoded.equalsIgnoreCase(encodedPassword);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
