package com.yarnshipment.discrepancy.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyUseCase;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.LiablePartyRequiredException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Uyuşmazlık uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = DiscrepancyDecisionController.class)
class DiscrepancyDecisionControllerTest {

    private static final String ONE = "/api/v1/discrepancies/" + TestDiscrepancies.ID;
    private static final String RESOLVE_BODY = "{\"resolution\":\"TOLERANCE\",\"note\":\"tolerans içinde\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private AssignDiscrepancyUseCase assignDiscrepancy;

    @MockitoBean
    private ResolveDiscrepancyUseCase resolveDiscrepancy;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DiscrepancyDecisionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Atama güncel uyuşmazlığı döner; çözülmüş uyuşmazlık DISCREPANCY_NOT_OPEN ile 409 alır")
    void shouldAssign() {
        String body = "{\"assigneeId\":\"" + UUID.randomUUID() + "\"}";
        when(assignDiscrepancy.assignDiscrepancy(any()))
                .thenReturn(TestDiscrepancies.withStatus(TestTokens.SITE_ID, DiscrepancyStatus.IN_REVIEW));

        assertThat(post(ONE + "/assign", body, quality())).hasStatusOk();

        when(assignDiscrepancy.assignDiscrepancy(any()))
                .thenThrow(new DiscrepancyNotOpenException(TestDiscrepancies.ID, DiscrepancyStatus.RESOLVED));
        MvcTestResult conflict = post(ONE + "/assign", body, quality());
        assertThat(conflict).hasStatus(HttpStatus.CONFLICT);
        assertThat(conflict).bodyJson().extractingPath("$.code").isEqualTo("DISCREPANCY_NOT_OPEN");
    }

    @Test
    @DisplayName("Karar sorumlu taraf eksikse LIABLE_PARTY_REQUIRED ile 400 alır; boş not 400 alır")
    void shouldValidateResolution() {
        when(resolveDiscrepancy.resolveDiscrepancy(any())).thenThrow(new LiablePartyRequiredException());

        MvcTestResult missingParty =
                post(ONE + "/resolve", "{\"resolution\":\"LIABILITY_ASSIGNED\",\"note\":\"sorumlu\"}", quality());
        MvcTestResult emptyNote = post(ONE + "/resolve", "{\"resolution\":\"TOLERANCE\",\"note\":\"\"}", quality());

        assertThat(missingParty).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(missingParty).bodyJson().extractingPath("$.code").isEqualTo("LIABLE_PARTY_REQUIRED");
        assertThat(emptyNote).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Karar güncel uyuşmazlığı döner")
    void shouldResolve() {
        when(resolveDiscrepancy.resolveDiscrepancy(any()))
                .thenReturn(TestDiscrepancies.withStatus(TestTokens.SITE_ID, DiscrepancyStatus.RESOLVED));

        assertThat(post(ONE + "/resolve", RESOLVE_BODY, quality())).hasStatusOk();
    }

    @Test
    @DisplayName("Okuma izni olan karar veremez; depo ve şoför de veremez")
    void shouldCheckPermissions() {
        Jwt manager = TestTokens.token("manager").build();

        assertThat(post(ONE + "/resolve", RESOLVE_BODY, manager)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(ONE + "/assign", "{\"assigneeId\":\"" + UUID.randomUUID() + "\"}", manager))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(
                        ONE + "/resolve",
                        RESOLVE_BODY,
                        TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(ONE + "/resolve", RESOLVE_BODY, TestTokens.driverToken()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        assertThat(mvc.post().uri(ONE + "/resolve").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private static Jwt quality() {
        return TestTokens.token("quality").build();
    }

    private MvcTestResult post(String uri, String body, Jwt jwt) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
