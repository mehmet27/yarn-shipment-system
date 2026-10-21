package com.yarnshipment.gateway;

import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Keycloak'a ulaşmadan çalışan istemci kaydı; çıkış adresi için {@code end_session_endpoint} verilir. */
@TestConfiguration(proxyBeanMethods = false)
class TestClientRegistrations {

    static final String END_SESSION_ENDPOINT =
            "http://keycloak.test/realms/yarnshipment/protocol/openid-connect/logout";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration keycloak = ClientRegistration.withRegistrationId("keycloak")
                .clientId("yarnshipment-gateway")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("http://keycloak.test/auth")
                .tokenUri("http://keycloak.test/token")
                .jwkSetUri("http://keycloak.test/certs")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", END_SESSION_ENDPOINT))
                .build();
        return new InMemoryClientRegistrationRepository(keycloak);
    }
}
