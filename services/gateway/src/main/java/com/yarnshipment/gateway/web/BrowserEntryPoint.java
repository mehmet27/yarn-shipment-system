package com.yarnshipment.gateway.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/** API çağrısı 401 problemi alır; tarayıcı sayfası Keycloak girişine yönlenir. */
public final class BrowserEntryPoint implements AuthenticationEntryPoint {

    private static final String API_PREFIX = "/api/";

    private final AuthenticationEntryPoint api;
    private final AuthenticationEntryPoint login;

    public BrowserEntryPoint(String loginPath, JsonMapper jsonMapper) {
        this.api = new AuthenticationRequiredEntryPoint(jsonMapper);
        this.login = new LoginUrlAuthenticationEntryPoint(loginPath);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        AuthenticationEntryPoint target = request.getRequestURI().startsWith(API_PREFIX) ? api : login;
        target.commence(request, response, exception);
    }
}
