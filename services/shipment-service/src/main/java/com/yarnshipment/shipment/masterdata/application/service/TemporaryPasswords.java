package com.yarnshipment.shipment.masterdata.application.service;

import java.security.SecureRandom;

/** Karışması kolay karakterleri (0/O, 1/l/I) içermeyen, okunup iletilebilir geçici şifre üretir. */
final class TemporaryPasswords {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {}

    static String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
