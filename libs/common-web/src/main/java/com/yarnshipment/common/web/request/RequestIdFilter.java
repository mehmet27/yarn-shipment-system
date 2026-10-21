package com.yarnshipment.common.web.request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Her isteğe bir {@code X-Request-Id} verir: istemcinin gönderdiği makul değer korunur, yoksa üretilir. Değer log
 * bağlamına, isteğe ve yanıt başlığına yazılır; hata yanıtları {@code requestId} alanında aynı değeri taşır.
 */
public final class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = accepted(request.getHeader(RequestIds.HEADER));
        request.setAttribute(RequestIds.ATTRIBUTE, requestId);
        response.setHeader(RequestIds.HEADER, requestId);
        MDC.put(RequestIds.MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIds.MDC_KEY);
        }
    }

    /** Log enjeksiyonunu önlemek için yalnızca kısa ve zararsız karakterli değerler kabul edilir. */
    private static String accepted(@Nullable String candidate) {
        if (candidate != null && ACCEPTED.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
