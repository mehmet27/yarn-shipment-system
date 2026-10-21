package com.yarnshipment.gateway.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Giriş başlatılırken {@code returnTo} adresini (güvenliyse) oturuma yazar; giriş bitince oraya dönülür. */
public final class ReturnToFilter extends OncePerRequestFilter {

    static final String SESSION_ATTRIBUTE = ReturnToFilter.class.getName() + ".RETURN_TO";
    private static final String PARAMETER = "returnTo";
    private static final String LOGIN_PREFIX = "/oauth2/authorization/";

    private final ReturnTarget target;

    public ReturnToFilter(String appUrl) {
        this.target = new ReturnTarget(appUrl);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(LOGIN_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        target.resolve(request.getParameter(PARAMETER))
                .ifPresent(address -> request.getSession().setAttribute(SESSION_ATTRIBUTE, address));
        chain.doFilter(request, response);
    }
}
