package com.yarnshipment.audit.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.audit.TestTokens;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsUseCase;
import com.yarnshipment.audit.application.port.in.ListChainChecksUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Denetim uçları yalnızca AUDIT_VIEW izni olanlara açıktır. */
@WebMvcTest(controllers = AuditLogController.class)
class AuditLogSecurityTest {

    private static final String AUDIT_LOG = "/api/v1/audit-log";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListAuditRecordsUseCase listAuditRecords;

    @MockitoBean
    private ListChainChecksUseCase listChainChecks;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    AuditLogSecurityTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Şoför denetim kayıtlarını göremez")
    void shouldRequireAuditViewPermission() {
        assertThat(mvc.get().uri(AUDIT_LOG).with(TestTokens.auth("driver")).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get()
                        .uri(AUDIT_LOG + "/chain-checks")
                        .with(TestTokens.auth("driver"))
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(listAuditRecords, listChainChecks);
    }

    @Test
    @DisplayName("Oturumsuz istek 401 döner")
    void shouldRequireAuthentication() {
        assertThat(mvc.get().uri(AUDIT_LOG).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
