package com.yarnshipment.gateway.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SPA'nın ilk GET'te {@code XSRF-TOKEN} çerezini alabilmesi için çerez yoksa token üretir ve yazar. Spring Security
 * token'ı ertelemeli yükler ve okunmadıkça çerezi yazmaz; bu filtre beklemeden yazar.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

    private final CookieCsrfTokenRepository repository;

    public CsrfCookieFilter(CookieCsrfTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (repository.loadToken(request) == null) {
            repository.saveToken(repository.generateToken(request), request, response);
        }
        chain.doFilter(request, response);
    }
}
