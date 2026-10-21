package com.yarnshipment.common.security;

import java.io.Serial;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** JWT ile doğrulanmış istek: ham token'ı (servisler arası aktarım için) ve çözülmüş {@link Actor}'ü taşır. */
public final class ActorAuthenticationToken extends JwtAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Actor actor;

    public ActorAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities, Actor actor) {
        super(jwt, authorities, actor.userId().toString());
        this.actor = actor;
    }

    public Actor actor() {
        return actor;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ActorAuthenticationToken token && super.equals(token) && actor.equals(token.actor);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + actor.hashCode();
    }
}
