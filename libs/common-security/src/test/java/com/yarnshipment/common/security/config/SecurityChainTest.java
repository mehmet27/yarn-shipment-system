package com.yarnshipment.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.TestTokens;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import com.yarnshipment.common.security.config.SecurityTestApplication.RecordingAuditPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
class SecurityChainTest {

    private final MockMvcTester mvc;
    private final RecordingAuditPublisher auditPublisher;

    @Autowired
    SecurityChainTest(MockMvcTester mvc, RecordingAuditPublisher auditPublisher) {
        this.mvc = mvc;
        this.auditPublisher = auditPublisher;
    }

    @BeforeEach
    void clearAudit() {
        auditPublisher.clear();
    }

    @Test
    @DisplayName("Oturumsuz istek 401 ve Türkçe problem cevabı alır")
    void shouldReturnProblemForMissingSession() {
        // when
        MvcTestResult result =
                mvc.get().uri("/test/shipments").header("X-Request-Id", "req-1").exchange();

        // then
        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Oturum gerekli");
        assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("req-1");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://errors.yarnshipment.com/authentication-required");
    }

    @Test
    @DisplayName("Bearer başlığı artık kimlik sayılmaz")
    void shouldIgnoreBearerHeader() {
        // when
        MvcTestResult result = mvc.get()
                .uri("/test/shipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer x")
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("İzni olan rol uca erişir")
    void shouldAllowPermittedRole() {
        // when / then
        assertThat(get("/test/shipments", "warehouse")).hasStatusOk().hasBodyTextEqualTo("[WAREHOUSE]");
    }

    @Test
    @DisplayName("İzni olmayan rol 403 alır ve ret audit'e yazılır")
    void shouldDenyAndAuditMissingPermission() {
        // when
        MvcTestResult result = mvc.post()
                .uri("/test/shipments")
                .with(sessionOf("quality"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCESS_DENIED");
        assertThat(auditPublisher.events())
                .extracting(SecurityAuditEvent::reason)
                .contains("MISSING_PERMISSION");
    }

    @Test
    @DisplayName("Durum değiştiren istek CSRF başlığı olmadan 403 alır")
    void shouldRejectStateChangeWithoutCsrfToken() {
        // when
        MvcTestResult result = mvc.post()
                .uri("/test/shipments")
                .with(sessionOf("warehouse"))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Başka lokasyonun kaydı 404 değil 403 OUT_OF_SCOPE döner")
    void shouldReturnForbiddenForOutOfScopeRecord() {
        // when
        MvcTestResult result = get("/test/shipments/in-site/" + TestTokens.OTHER_SITE_ID, "warehouse");

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("OUT_OF_SCOPE");
        assertThat(auditPublisher.events())
                .extracting(SecurityAuditEvent::reason)
                .contains("OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("Kullanıcı kendi lokasyonunun kaydına erişir")
    void shouldAllowOwnRecord() {
        // when / then
        assertThat(get("/test/shipments/in-site/" + TestTokens.SITE_ID, "warehouse"))
                .hasStatusOk();
    }

    @Test
    @DisplayName("Sağlık ucu oturum istemez")
    void shouldPermitPublicPaths() {
        // when / then
        assertThat(mvc.get().uri("/actuator/info").exchange().getResponse().getStatus())
                .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private MvcTestResult get(String uri, String role) {
        return mvc.get()
                .uri(uri)
                .with(sessionOf(role))
                .exchange();
    }

    private static RequestPostProcessor sessionOf(String role) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                TestTokens.CONVERTER.convert(TestTokens.withRoles(role).build()));
    }
}
