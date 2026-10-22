package com.yarnshipment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest(properties = "GATEWAY_CLIENT_SECRET=test-secret")
@AutoConfigureMockMvc
@Import(TestClientRegistrations.class)
class GatewayBrowserSessionTest {

    private final MockMvcTester mvc;
    private final ClientRegistrationRepository registrations;
    private final StringRedisTemplate redis;

    @Autowired
    GatewayBrowserSessionTest(
            MockMvcTester mvc, ClientRegistrationRepository registrations, StringRedisTemplate redis) {
        this.mvc = mvc;
        this.redis = redis;
        this.registrations = registrations;
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        TestRedis.register(registry);
    }

    @Test
    @DisplayName("Oturum çerezi verilen istekte oturum Redis'e yazılır")
    void shouldStoreSessionInRedis() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).startsWith("SESSION="));
        assertThat(redis.keys("yarnshipment:gateway:session:sessions:*")).isNotEmpty();
    }

    @Test
    @DisplayName("Oturumsuz API çağrısına AUTHENTICATION_REQUIRED problemi ile 401 verilir")
    void shouldReturnProblemForAnonymousApiCall() {
        MvcTestResult result = mvc.get().uri("/api/v1/me").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("Oturumsuz sayfa isteği Keycloak girişine yönlendirilir")
    void shouldRedirectAnonymousPageToLogin() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result).headers().hasValue(HttpHeaders.LOCATION, "/oauth2/authorization/keycloak");
    }

    @Test
    @DisplayName("İlk istekte okunabilir XSRF-TOKEN çerezi verilir")
    void shouldIssueReadableCsrfCookie() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(
                        header -> assertThat(header).startsWith("XSRF-TOKEN=").doesNotContain("HttpOnly"));
    }

    @Test
    @DisplayName("CSRF başlığı olmayan çıkış reddedilir")
    void shouldRejectLogoutWithoutCsrfToken() {
        MvcTestResult result = mvc.post()
                .uri("/logout")
                .with(SecurityMockMvcRequestPostProcessors.oidcLogin())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Çıkış, Keycloak oturumunu kapatacak adresi JSON olarak döner")
    void shouldReturnKeycloakLogoutUrl() {
        MvcTestResult result = mvc.post()
                .uri("/logout")
                .with(SecurityMockMvcRequestPostProcessors.oidcLogin()
                        .clientRegistration(registrations.findByRegistrationId("keycloak")))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyText()
                .contains(TestClientRegistrations.END_SESSION_ENDPOINT)
                .contains("post_logout_redirect_uri=http://localhost:5173/");
    }
}
