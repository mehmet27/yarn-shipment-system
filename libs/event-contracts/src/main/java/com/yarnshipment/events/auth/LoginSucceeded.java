package com.yarnshipment.events.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * GirişYapıldı: kişi doğru şifreyle oturum açtı.
 *
 * @param userId giriş yapan hesap
 * @param username giriş adı
 * @param occurredAt giriş zamanı
 */
public record LoginSucceeded(UUID userId, String username, Instant occurredAt) {

    public static final String TYPE = "com.yarnshipment.auth.login-succeeded.v1";
}
