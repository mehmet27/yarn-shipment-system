package com.yarnshipment.shipment.masterdata.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Altı haneli SMS kodunu üretir ve tuzlu özetini alır; kodun kendisi saklanmaz. */
final class ResetCodes {

    private static final int CODE_BOUND = 1_000_000;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ResetCodes() {}

    static String generate() {
        return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
    }

    static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String hash(String salt, String code) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /** Süreyi sızdırmayan karşılaştırma. */
    static boolean matches(String salt, String code, String expectedHash) {
        return MessageDigest.isEqual(
                hash(salt, code).getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
