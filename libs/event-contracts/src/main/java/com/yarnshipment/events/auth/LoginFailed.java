package com.yarnshipment.events.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * HatalıGirişDenemesi: var olan bir hesaba yanlış şifreyle girilmeye çalışıldı. Olmayan giriş adıyla yapılan
 * denemeler yayınlanmaz: yazılan metin çoğu zaman yanlış kutuya girilmiş bir şifredir ve denetim kaydı silinemez.
 *
 * @param userId denenen hesap
 * @param username giriş adı
 * @param occurredAt deneme zamanı
 */
public record LoginFailed(UUID userId, String username, Instant occurredAt) {

    public static final String TYPE = "com.yarnshipment.auth.login-failed.v1";
}
