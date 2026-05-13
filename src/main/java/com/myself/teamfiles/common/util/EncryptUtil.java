package com.myself.teamfiles.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EncryptUtil {

    private static final String ALGORITHM = "AES";
    private static final String SECRET;

    static {
        String key = System.getenv("AI_ENCRYPT_KEY");
        if (key == null || key.isEmpty()) {
            key = "teamfiles-aes-key-256bit-padding!";
        }
        // Pad/truncate to 32 bytes for AES-256
        byte[] keyBytes = new byte[32];
        byte[] src = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
        SECRET = Base64.getEncoder().encodeToString(keyBytes);
    }

    private EncryptUtil() {}

    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(SECRET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(SECRET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }
}
