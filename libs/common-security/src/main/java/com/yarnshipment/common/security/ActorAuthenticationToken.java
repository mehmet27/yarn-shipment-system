package com.yarnshipment.common.security;

import java.io.Serial;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Oturumla doğrulanmış istek: çözülmüş {@link Actor}'ü, görünen adı ve "şifre değiştirme zorunlu" durumunu taşır.
 * Şifre değiştirmesi gereken kullanıcının yetkisi yoktur (rol ve izin boş); yalnızca kendi şifresini değiştirebilir.
 */
public final class ActorAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 2L;

    private final Actor actor;
    private final String fullName;
    private final boolean passwordChangeRequired;

    public ActorAuthenticationToken(
            Collection<? extends GrantedAuthority> authorities,
            Actor actor,
            String fullName,
            boolean passwordChangeRequired) {
        super(authorities);
        this.actor = actor;
        this.fullName = fullName;
        this.passwordChangeRequired = passwordChangeRequired;
        setAuthenticated(true);
    }

    public Actor actor() {
        return actor;
    }

    /** Ad soyad; yoksa giriş adı. */
    public String fullName() {
        return fullName;
    }

    public boolean passwordChangeRequired() {
        return passwordChangeRequired;
    }

    @Override
    public Object getPrincipal() {
        return actor;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public String getName() {
        return actor.userId().toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ActorAuthenticationToken token
                && super.equals(token)
                && actor.equals(token.actor)
                && passwordChangeRequired == token.passwordChangeRequired;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + actor.hashCode();
    }
}
