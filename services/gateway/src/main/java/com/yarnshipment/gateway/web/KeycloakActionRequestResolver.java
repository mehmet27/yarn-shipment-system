package com.yarnshipment.gateway.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Girişi başlatan isteğe {@code kc_action=UPDATE_PASSWORD} gelirse Keycloak'a aynen iletir; Keycloak kişiye kendi
 * "şifre değiştir" sayfasını gösterir ve bitince uygulamaya döndürür. Şifre bu uygulamadan hiç geçmez. Başka eylem
 * değerleri iletilmez (yalnızca izin verilen tek eylem).
 */
public final class KeycloakActionRequestResolver implements OAuth2AuthorizationRequestResolver {

    static final String PARAMETER = "kc_action";
    static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private final OAuth2AuthorizationRequestResolver delegate;

    private KeycloakActionRequestResolver(OAuth2AuthorizationRequestResolver delegate) {
        this.delegate = delegate;
    }

    public static KeycloakActionRequestResolver forRegistrations(ClientRegistrationRepository registrations) {
        return new KeycloakActionRequestResolver(
                new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization"));
    }

    @Override
    public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withAction(delegate.resolve(request), request);
    }

    @Override
    public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withAction(delegate.resolve(request, clientRegistrationId), request);
    }

    private static @Nullable OAuth2AuthorizationRequest withAction(
            @Nullable OAuth2AuthorizationRequest authorization, HttpServletRequest request) {
        if (authorization == null || !UPDATE_PASSWORD.equals(request.getParameter(PARAMETER))) {
            return authorization;
        }
        return OAuth2AuthorizationRequest.from(authorization)
                .additionalParameters(parameters -> parameters.put(PARAMETER, UPDATE_PASSWORD))
                .build();
    }
}
