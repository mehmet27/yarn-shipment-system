package com.yarnshipment.common.web.idempotency;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** İsteği yapan kimliğin adı; oturumsuz veya anonim istekte {@code null}. */
final class ActorNames {

    private ActorNames() {}

    static @Nullable String current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
