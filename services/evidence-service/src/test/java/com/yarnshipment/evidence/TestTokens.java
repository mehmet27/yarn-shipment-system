package com.yarnshipment.evidence;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.KeycloakJwtConverter;
import com.yarnshipment.common.security.SecurityProperties;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Testlerde Keycloak token'ı yerine geçen JWT'ler ve bunlardan çözülen aktörler. */
public final class TestTokens {

    public static final UUID SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    public static final UUID SUBCONTRACTOR_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    public static final UUID DRIVER_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    public static final KeycloakJwtConverter CONVERTER =
            new KeycloakJwtConverter(new SecurityProperties(Set.of(), Set.of("otp"), Set.of("2"), List.of()));

    private TestTokens() {}

    /** Verilen rollerle, lokasyonu {@link #SITE_ID} olan token. */
    public static Jwt.Builder token(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("preferred_username", "test.user")
                .claim("name", "Test Kullanıcı")
                .claim("roles", List.of(roles))
                .claim("site_ids", List.of(SITE_ID.toString()));
    }

    public static Jwt subcontractorToken() {
        return token("subcontractor")
                .claim("subcontractor_id", SUBCONTRACTOR_ID.toString())
                .build();
    }

    public static Jwt driverToken() {
        return token("driver").claim("driver_id", DRIVER_ID.toString()).build();
    }

    public static Actor actor(Jwt jwt) {
        return CONVERTER.toActor(jwt);
    }

    public static Actor actor(String... roles) {
        return CONVERTER.toActor(token(roles).build());
    }

    /** MFA ile açılmış oturumun aktörü. */
    public static Actor mfaActor(String... roles) {
        return CONVERTER.toActor(token(roles).claim("amr", List.of("otp")).build());
    }
}
