package com.yarnshipment.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Testlerde oturum kimliği yerine geçen test JWT'leri. */
public final class TestTokens {

    public static final UUID SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    public static final UUID OTHER_SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    public static final Converter CONVERTER = new Converter();

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
                .claim("preferred_username", "test.user")
                .claim("roles", List.of(roles))
                .claim("site_ids", List.of(SITE_ID.toString()));
    }

    /** MFA ile açılmış oturumun token'ı. */
    public static Jwt.Builder withMfa(String... roles) {
        return withRoles(roles).claim("amr", List.of("pwd", "otp"));
    }

    /** Test JWT'sini oturum kimliğine çevirir. Üretimde JWT yoktur; {@link Jwt} burada yalnızca test verisi taşır. */
    public static final class Converter {

        public ActorAuthenticationToken convert(Jwt jwt) {
            Actor actor = toActor(jwt);
            List<GrantedAuthority> authorities = new ArrayList<>(actor.permissions());
            actor.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.authority())));
            String name = jwt.getClaimAsString("name");
            return new ActorAuthenticationToken(authorities, actor, name == null ? actor.username() : name, false);
        }

        public Actor toActor(Jwt jwt) {
            Set<Role> roles = EnumSet.noneOf(Role.class);
            List<String> values = jwt.getClaimAsStringList("roles");
            if (values != null) {
                values.forEach(value -> Role.fromClaim(value).ifPresent(roles::add));
            }
            List<String> amr = jwt.getClaimAsStringList("amr");
            List<String> sites = jwt.getClaimAsStringList("site_ids");
            String username = jwt.getClaimAsString("preferred_username");
            return new Actor(
                    UUID.fromString(jwt.getSubject()),
                    username == null ? jwt.getSubject() : username,
                    roles,
                    RolePermissionMatrix.permissionsOf(roles),
                    sites == null ? Set.of() : Set.copyOf(sites.stream().map(UUID::fromString).toList()),
                    amr != null && amr.contains("otp"));
        }
    }
}
