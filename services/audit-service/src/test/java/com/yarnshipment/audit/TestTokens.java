package com.yarnshipment.audit;

import com.yarnshipment.common.security.KeycloakJwtConverter;
import com.yarnshipment.common.security.SecurityProperties;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Testlerde Keycloak token'ı yerine geçen JWT'ler. */
public final class TestTokens {

    public static final KeycloakJwtConverter CONVERTER =
            new KeycloakJwtConverter(new SecurityProperties(Set.of(), Set.of("otp"), Set.of("2"), List.of()));

    private TestTokens() {}

    /** Verilen rollerle token. */
    public static Jwt.Builder token(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("preferred_username", "test.user")
                .claim("name", "Test Kullanıcı")
                .claim("roles", List.of(roles));
    }

    /** Verilen rolle oturum açmış istek. */
    public static RequestPostProcessor auth(String role) {
        return SecurityMockMvcRequestPostProcessors.authentication(CONVERTER.convert(token(role)
                .claim("driver_id", UUID.randomUUID().toString())
                .claim("subcontractor_id", UUID.randomUUID().toString())
                .build()));
    }
}
