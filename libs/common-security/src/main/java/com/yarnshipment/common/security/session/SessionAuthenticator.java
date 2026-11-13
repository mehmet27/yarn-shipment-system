package com.yarnshipment.common.security.session;

import com.yarnshipment.common.security.ActorAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Doğrulanmış kullanıcıyı oturuma yazar: oturum kimliğini yeniler (oturum sabitleme koruması), kaydı oturum
 * defterine ekler ve güvenlik bağlamını kaydeder. Giriş ve şifre değiştirme sonrası kullanılır.
 */
public final class SessionAuthenticator {

    private final SecurityContextRepository repository;
    private final SessionAuthenticationStrategy strategy;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public SessionAuthenticator(SessionRegistry registry) {
        this.repository = new HttpSessionSecurityContextRepository();
        this.strategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new RegisterSessionAuthenticationStrategy(registry)));
    }

    /** Kullanıcıyı bu isteğin oturumuna yazar; yeni oturum kimliği verilir. */
    public void establish(
            ActorAuthenticationToken authentication, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        strategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        repository.saveContext(context, request, response);
    }

    /** Oturumu kapatır ve bağlamı temizler. */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
