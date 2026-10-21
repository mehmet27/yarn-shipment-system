package com.yarnshipment.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Testlerde Keycloak token'ı yerine geçen JWT'ler. */
public final class TestTokens {

    public static final UUID SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    public static final UUID OTHER_SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    public static final UUID SUBCONTRACTOR_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    public static final UUID DRIVER_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");

    private static final Instant ISSUED_AT = Instant.parse("2026-10-21T08:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(5);

    private TestTokens() {}

    /** Verilen rollerle, lokasyon kapsamı {@link #SITE_ID} olan token. */
    public static Jwt.Builder withRoles(String... roles) {
        return Jwt.withTokenValue("token-" + String.join("-", roles))
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(ISSUED_AT)
                .expiresAt(ISSUED_AT.plus(LIFETIME))
                .claim(TokenClaims.PREFERRED_USERNAME, "test.user")
                .claim(TokenClaims.ROLES, List.of(roles))
                .claim(TokenClaims.SITE_IDS, List.of(SITE_ID.toString()));
    }

    /** MFA ile açılmış oturumun token'ı. */
    public static Jwt.Builder withMfa(String... roles) {
        return withRoles(roles).claim(TokenClaims.AMR, List.of("pwd", "otp"));
    }

    public static Jwt subcontractor() {
        return withRoles("subcontractor")
                .claim(TokenClaims.SUBCONTRACTOR_ID, SUBCONTRACTOR_ID.toString())
                .build();
    }

    public static Jwt driver() {
        return withRoles("driver")
                .claim(TokenClaims.DRIVER_ID, DRIVER_ID.toString())
                .build();
    }
}
