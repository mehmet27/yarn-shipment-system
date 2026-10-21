package com.yarnshipment.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.TestTokens;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import com.yarnshipment.common.security.config.SecurityTestApplication.RecordingAuditPublisher;
import com.yarnshipment.common.security.config.SecurityTestApplication.StubJwtDecoder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
class SecurityChainTest {

    private final MockMvcTester mvc;
    private final StubJwtDecoder jwtDecoder;
    private final RecordingAuditPublisher auditPublisher;

    @Autowired
    SecurityChainTest(MockMvcTester mvc, StubJwtDecoder jwtDecoder, RecordingAuditPublisher auditPublisher) {
        this.mvc = mvc;
        this.jwtDecoder = jwtDecoder;
        this.auditPublisher = auditPublisher;
    }

    @BeforeEach
    void clearAudit() {
        auditPublisher.clear();
    }

    @Test
    @DisplayName("Token'sız istek 401 ve Türkçe problem cevabı alır")
    void shouldReturnProblemForMissingToken() {
        // when
        MvcTestResult result =
                mvc.get().uri("/test/shipments").header("X-Request-Id", "req-1").exchange();

        // then
        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Oturum gerekli");
        assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("req-1");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("https://errors.yarnshipment.com/authentication-required");
    }

    @Test
    @DisplayName("Geçersiz token 401 alır")
    void shouldRejectUnknownToken() {
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
    @DisplayName("Kural ihlali olan token (karışık roller) 401 alır")
    void shouldRejectTokenViolatingRoleRules() {
        // given
        String token = jwtDecoder.register(
                TestTokens.withRoles("driver", "subcontractor").build());

        // when / then
        assertThat(get("/test/shipments", token)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("İzni olan rol uca erişir")
    void shouldAllowPermittedRole() {
        // given
        String token = jwtDecoder.register(TestTokens.withRoles("warehouse").build());

        // when / then
        assertThat(get("/test/shipments", token)).hasStatusOk().hasBodyTextEqualTo("[WAREHOUSE]");
    }

    @Test
    @DisplayName("İzni olmayan rol 403 alır ve ret audit'e yazılır")
    void shouldDenyAndAuditMissingPermission() {
        // given
        String token = jwtDecoder.register(TestTokens.withRoles("manager").build());

        // when
        MvcTestResult result = mvc.post()
                .uri("/test/shipments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCESS_DENIED");
        assertThat(auditPublisher.events())
                .extracting(SecurityAuditEvent::reason)
                .contains("MISSING_PERMISSION");
    }

    @Test
    @DisplayName("Başka fasoncunun kaydı 404 değil 403 OUT_OF_SCOPE döner")
    void shouldReturnForbiddenForOutOfScopeRecord() {
        // given
        String token = jwtDecoder.register(TestTokens.subcontractor());

        // when
        MvcTestResult result = get("/test/shipments/of-subcontractor/" + UUID.randomUUID(), token);

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("OUT_OF_SCOPE");
        assertThat(auditPublisher.events())
                .extracting(SecurityAuditEvent::reason)
                .contains("OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("Fasoncu kendi kaydına erişir")
    void shouldAllowOwnRecord() {
        // given
        String token = jwtDecoder.register(TestTokens.subcontractor());

        // when / then
        assertThat(get("/test/shipments/of-subcontractor/" + TestTokens.SUBCONTRACTOR_ID, token))
                .hasStatusOk();
    }

    @Test
    @DisplayName("Sağlık ucu token istemez")
    void shouldPermitPublicPaths() {
        // when / then
        assertThat(mvc.get().uri("/actuator/info").exchange().getResponse().getStatus())
                .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }
}
