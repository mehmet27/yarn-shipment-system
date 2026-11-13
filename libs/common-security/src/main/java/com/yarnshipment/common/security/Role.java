package com.yarnshipment.common.security;

import java.util.Locale;
import java.util.Optional;

/**
 * Uygulama rolleri (ön analiz Bölüm 6.1, yetkilendirme analizi). Fasoncu giriş yapmaz (KR-17);
 * fasoncu yalnızca ana veridir.
 */
public enum Role {
    ADMIN,
    WAREHOUSE,
    QUALITY,
    /** Müdür: sevkiyatı oluşturur, iptal eder ve irsaliye gelince tamamlar; raporları görür. */
    MANAGER,
    /** Genel müdür: yalnızca rapor ve bakiye görür; işlem yapmaz. */
    GENERAL_MANAGER,
    /** Sistem işlemleri için ayrılmıştır; giriş yapan kullanıcıya verilmez. */
    SYSTEM;

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /** Rol adını çevirir (büyük/küçük harf duyarsız); tanınmayan ad boş döner. */
    public static Optional<Role> fromClaim(String claimValue) {
        String normalized = claimValue.toUpperCase(Locale.ROOT);
        for (Role role : values()) {
            if (role.name().equals(normalized)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    /** Spring Security yetki adı, ör. {@code ROLE_ADMIN}. */
    public String authority() {
        return AUTHORITY_PREFIX + name();
    }
}
