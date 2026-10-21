package com.yarnshipment.gateway.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Oturumsuz API çağrısına yönlendirme yerine ERR-07 biçiminde 401 verir; SPA bunu görünce girişi kendisi başlatır.
 * Gövde, servislerin {@code AUTHENTICATION_REQUIRED} problemiyle aynı alanları taşır.
 */
public final class AuthenticationRequiredEntryPoint implements AuthenticationEntryPoint {

    static final String CODE = "AUTHENTICATION_REQUIRED";

    private final JsonMapper jsonMapper;

    public AuthenticationRequiredEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body(request));
    }

    private static Map<String, Object> body(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://errors.yarnshipment.com/authentication-required");
        body.put("title", "Oturum gerekli");
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("detail", "Oturumunuz yok veya süresi dolmuş. Lütfen yeniden giriş yapın.");
        body.put("instance", request.getRequestURI());
        body.put("code", CODE);
        return body;
    }
}
