package com.yarnshipment.events.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * HesapKilitlendi: art arda yanlış denemeler yüzünden hesap bir süre girişe kapandı.
 *
 * @param userId kilitlenen hesap
 * @param username giriş adı
 * @param lockedUntil kilidin kalkacağı an
 * @param occurredAt kilitlenme zamanı
 */
public record AccountLocked(UUID userId, String username, Instant lockedUntil, Instant occurredAt) {

    public static final String TYPE = "com.yarnshipment.auth.account-locked.v1";
}
