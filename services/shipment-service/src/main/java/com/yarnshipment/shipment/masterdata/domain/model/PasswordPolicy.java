package com.yarnshipment.shipment.masterdata.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Şifre kuralı: en az 8 karakter, en çok 128 karakter ve UTF-8 olarak en çok 72 bayt; yalnızca tek karakterin
 * tekrarı, giriş adı ve sık kullanılan kolay şifreler kabul edilmez. BCrypt şifrenin yalnızca ilk 72 baytını kullanır:
 * daha uzunu özetlenemez (hata verir) ve fazlası zaten korumaya katılmaz; bu yüzden baştan reddedilir. Kural
 * sunucudadır; arayüzdeki ipuçları yalnızca kolaylıktır.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    /** BCrypt'in sınırı; Türkçe harfler UTF-8'de iki bayttır. */
    public static final int MAX_BYTES = 72;

    private static final Set<String> COMMON = Set.of(
            "12345678",
            "123456789",
            "1234567890",
            "11111111",
            "00000000",
            "87654321",
            "qwertyui",
            "qwerty123",
            "password",
            "password1",
            "password123",
            "parola123",
            "sifre123",
            "sifre1234",
            "iplik123",
            "iplik1234",
            "admin123",
            "admin1234",
            "abc12345",
            "abcd1234",
            "asdfghjk",
            "12341234",
            "11223344");

    private PasswordPolicy() {}

    /** Şifre kurala uymuyorsa {@link WeakPasswordException} fırlatır. */
    public static void require(String username, String password) {
        if (password.length() < MIN_LENGTH
                || password.length() > MAX_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new WeakPasswordException();
        }
        String lower = password.toLowerCase(Locale.ROOT);
        boolean oneCharacter = password.chars().distinct().count() == 1;
        if (oneCharacter
                || COMMON.contains(lower)
                || lower.equals(username.strip().toLowerCase(Locale.ROOT))) {
            throw new WeakPasswordException();
        }
    }
}
