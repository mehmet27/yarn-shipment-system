package com.yarnshipment.gateway.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** Giriş sonrası, girişi başlatan sayfaya; kaydı yoksa SPA köküne yönlendirir. */
public final class ReturnToSuccessHandler implements AuthenticationSuccessHandler {

    private final String defaultUrl;

    public ReturnToSuccessHandler(String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object saved = session == null ? null : session.getAttribute(ReturnToFilter.SESSION_ATTRIBUTE);
        if (saved instanceof String address) {
            session.removeAttribute(ReturnToFilter.SESSION_ATTRIBUTE);
            response.sendRedirect(address);
            return;
        }
        response.sendRedirect(defaultUrl);
    }
}
