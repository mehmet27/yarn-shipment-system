package com.yarnshipment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Oturumdaki kullanıcının izinlerini her istekte rolünün güncel izinleriyle yeniler. Yönetici bir rolün iznini
 * değiştirince açık oturumlar yeniden giriş istemeden yeni izinle çalışır. Şifre değiştirmesi gereken kullanıcının
 * rolü yoktur, olduğu gibi bırakılır.
 */
public final class PermissionRefreshFilter extends OncePerRequestFilter {

    /** Log bağlamındaki kullanıcı kimliği anahtarı. */
    public static final String USER_ID_MDC_KEY = "userId";

    private final Supplier<RolePermissionProvider> provider;

    public PermissionRefreshFilter(Supplier<RolePermissionProvider> provider) {
        this.provider = provider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication instanceof ActorAuthenticationToken token
                && !token.passwordChangeRequired()
                && !token.actor().roles().isEmpty()) {
            Set<Permission> fresh = provider.get().permissionsOf(token.actor().roles());
            if (!fresh.equals(token.actor().permissions())) {
                context.setAuthentication(refreshed(token, fresh));
            }
        }
        proceedWithUserInLogContext(request, response, chain);
    }

    /** Log satırları işlemi yapanın kimliğini taşır (ad değil, SEC-12); oturumsuz istekte alan yoktur. */
    private static void proceedWithUserInLogContext(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (!(current instanceof ActorAuthenticationToken token)) {
            chain.doFilter(request, response);
            return;
        }
        MDC.put(USER_ID_MDC_KEY, token.actor().userId().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    private static ActorAuthenticationToken refreshed(ActorAuthenticationToken token, Set<Permission> permissions) {
        Actor old = token.actor();
        Actor actor =
                new Actor(old.userId(), old.username(), old.roles(), permissions, old.siteIds(), old.mfaVerified());
        List<GrantedAuthority> authorities = new ArrayList<>(permissions);
        actor.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.authority())));
        return new ActorAuthenticationToken(authorities, actor, token.fullName(), token.passwordChangeRequired());
    }
}
