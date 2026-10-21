package com.yarnshipment.gateway.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gateway oturumunu kapattıktan sonra Keycloak oturumunu kapatacak adresi JSON olarak verir. SPA'nın {@code fetch}
 * çağrısı Keycloak'a yönlendirilemeyeceği için yönlendirmeyi tarayıcı yapar: {@code {"logoutUrl": "..."}}.
 */
public final class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

    private final LogoutSuccessHandler delegate;

    public JsonLogoutSuccessHandler(
            ClientRegistrationRepository registrations, String postLogoutRedirectUri, JsonMapper jsonMapper) {
        OidcClientInitiatedLogoutSuccessHandler oidc = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        oidc.setPostLogoutRedirectUri(postLogoutRedirectUri);
        // Yönlendirme yerine adres gövdeye yazılır.
        oidc.setRedirectStrategy((request, response, url) -> writeUrl(response, url, jsonMapper));
        this.delegate = oidc;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, @Nullable Authentication authentication)
            throws IOException, ServletException {
        delegate.onLogoutSuccess(request, response, authentication);
    }

    private static void writeUrl(HttpServletResponse response, String logoutUrl, JsonMapper jsonMapper)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), Map.of("logoutUrl", logoutUrl));
    }
}
