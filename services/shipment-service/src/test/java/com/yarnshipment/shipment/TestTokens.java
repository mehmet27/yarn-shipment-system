package com.yarnshipment.shipment;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.ActorAuthenticationToken;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Testlerde oturum kimliği yerine geçen test JWT'leri ve bunlardan çözülen aktörler. */
public final class TestTokens {

    public static final UUID SITE_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    public static final UUID SUBCONTRACTOR_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    public static final Converter CONVERTER = new Converter();

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
