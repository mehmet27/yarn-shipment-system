package com.yarnshipment.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** O anki isteğin aktörü. Controller aktörü buradan alıp komuta parametre olarak geçirir (CONC-05). */
public final class CurrentActorProvider {

    /** Doğrulanmış aktör; yoksa (filtre zinciri dışında çağrı) istisna fırlatır. */
    public Actor getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ActorAuthenticationToken token) {
            return token.actor();
        }
        throw new AuthenticationCredentialsNotFoundException("Request has no authenticated actor");
    }
}
