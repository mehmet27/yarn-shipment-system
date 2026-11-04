package com.yarnshipment.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/** Profilden şifre değiştirme: yalnızca izin verilen tek Keycloak eylemi girişe eklenir. */
class KeycloakActionRequestResolverTest {

    private static final String LOGIN = "/oauth2/authorization/keycloak";

    private final KeycloakActionRequestResolver resolver =
            KeycloakActionRequestResolver.forRegistrations(new InMemoryClientRegistrationRepository(registration()));

    @Test
    @DisplayName("kc_action=UPDATE_PASSWORD Keycloak yetkilendirme isteğine aynen eklenir")
    void shouldForwardUpdatePassword() {
        OAuth2AuthorizationRequest authorization = resolved(request("UPDATE_PASSWORD"));

        assertThat(authorization.getAdditionalParameters()).containsEntry("kc_action", "UPDATE_PASSWORD");
        assertThat(authorization.getAuthorizationRequestUri()).contains("kc_action=UPDATE_PASSWORD");
    }

    @Test
    @DisplayName("Başka eylem değerleri ve eylemsiz giriş değişmeden geçer")
    void shouldIgnoreOtherActions() {
        OAuth2AuthorizationRequest other = resolved(request("DELETE_ACCOUNT"));
        OAuth2AuthorizationRequest none = resolved(request());

        assertThat(other.getAdditionalParameters()).doesNotContainKey("kc_action");
        assertThat(none.getAdditionalParameters()).doesNotContainKey("kc_action");
    }

    @Test
    @DisplayName("Giriş yolu dışındaki istekte yetkilendirme isteği üretilmez")
    void shouldNotResolveOtherPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.setServletPath("/api/v1/me");

        assertThat(resolver.resolve(request)).isNull();
    }

    private OAuth2AuthorizationRequest resolved(MockHttpServletRequest request) {
        return Optional.ofNullable(resolver.resolve(request)).orElseThrow();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", LOGIN);
        request.setServletPath(LOGIN);
        return request;
    }

    private static MockHttpServletRequest request(String action) {
        MockHttpServletRequest request = request();
        request.setParameter("kc_action", action);
        return request;
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("gateway")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/keycloak")
                .scope("openid")
                .authorizationUri("http://kc/auth")
                .tokenUri("http://kc/token")
                .build();
    }
}
