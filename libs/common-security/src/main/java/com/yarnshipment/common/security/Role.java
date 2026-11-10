package com.yarnshipment.common.security;

import java.util.Locale;
import java.util.Optional;

/**
 * Keycloak realm rolleri (ön analiz Bölüm 6.1, yetkilendirme analizi). Dış rol (fasoncu) fabrika içi rollerle
 * aynı hesapta birleştirilemez.
 */
public enum Role {
    ADMIN(Party.INTERNAL),
    WAREHOUSE(Party.INTERNAL),
    SUBCONTRACTOR(Party.EXTERNAL),
    QUALITY(Party.INTERNAL),
    /** Müdür: sevkiyatı oluşturur, iptal eder ve irsaliye gelince tamamlar; raporları görür. */
    MANAGER(Party.INTERNAL),
    /** Yalnızca servis hesaplarının (client credentials) token'larında bulunur. */
    SYSTEM(Party.INTERNAL);

    private static final String AUTHORITY_PREFIX = "ROLE_";

    private final Party party;

    Role(Party party) {
        this.party = party;
    }

    /** Keycloak'tan gelen rol adını çevirir; tanınmayan roller ({@code offline_access} vb.) yok sayılır. */
    public static Optional<Role> fromClaim(String claimValue) {
        String normalized = claimValue.toUpperCase(Locale.ROOT);
        for (Role role : values()) {
            if (role.name().equals(normalized)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    public boolean isExternal() {
        return party == Party.EXTERNAL;
    }

    /** Spring Security yetki adı, ör. {@code ROLE_ADMIN}. */
    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    private enum Party {
        INTERNAL,
        EXTERNAL
    }
}
