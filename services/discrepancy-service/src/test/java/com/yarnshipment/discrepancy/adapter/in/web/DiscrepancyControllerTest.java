package com.yarnshipment.discrepancy.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesUseCase;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepancyActivityUseCase;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotFoundException;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Uyuşmazlık uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = DiscrepancyController.class)
class DiscrepancyControllerTest {

    private static final int PAGE_SIZE = 20;
    private static final String BASE = "/api/v1/discrepancies";
    private static final String ONE = BASE + "/" + TestDiscrepancies.ID;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListDiscrepanciesUseCase listDiscrepancies;

    @MockitoBean
    private GetDiscrepancyUseCase getDiscrepancy;

    @MockitoBean
    private ListDiscrepancyActivityUseCase listActivity;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DiscrepancyControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Liste sayfa bilgisini ve kilo değerlerini ondalık dize olarak döner")
    void shouldListDiscrepancies() {
        when(listDiscrepancies.listDiscrepancies(any()))
                .thenReturn(new DiscrepancyPage(List.of(TestDiscrepancies.open(TestTokens.SITE_ID)), 0, PAGE_SIZE, 1));

        MvcTestResult result = get(BASE + "?status=OPEN&severity=RED", quality());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.items[0].diffKg").isEqualTo("40.000");
        assertThat(result).bodyJson().extractingPath("$.items[0].severity").isEqualTo("RED");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.items[0].allowedActions")
                .isEqualTo(List.of("ASSIGN", "RESOLVE"));
    }

    @Test
    @DisplayName("Yönetici uyuşmazlığı görür ama eylem butonu göremez")
    void shouldHideActionsFromReadOnlyRole() {
        when(getDiscrepancy.getDiscrepancy(any())).thenReturn(TestDiscrepancies.open(TestTokens.SITE_ID));

        MvcTestResult result = get(ONE, TestTokens.token("manager").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.allowedActions").isEqualTo(List.of());
    }

    @Test
    @DisplayName("Çözülmüş uyuşmazlık kararı ve çözen kişiyi taşır")
    void shouldShowDecision() {
        UUID user = UUID.randomUUID();
        Discrepancy resolved = TestDiscrepancies.open(TestTokens.SITE_ID)
                .resolve(new Discrepancy.Decision(Resolution.TOLERANCE, "uygun", null, user, Instant.now()));
        when(getDiscrepancy.getDiscrepancy(any())).thenReturn(resolved);

        MvcTestResult result = get(ONE, quality());

        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("RESOLVED");
        assertThat(result).bodyJson().extractingPath("$.resolution").isEqualTo("TOLERANCE");
        assertThat(result).bodyJson().extractingPath("$.resolvedBy").isEqualTo(user.toString());
    }

    @Test
    @DisplayName("Olmayan uyuşmazlık DISCREPANCY_NOT_FOUND koduyla 404 döner")
    void shouldReturnNotFound() {
        when(getDiscrepancy.getDiscrepancy(any())).thenThrow(new DiscrepancyNotFoundException(TestDiscrepancies.ID));

        MvcTestResult result = get(ONE, quality());

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("DISCREPANCY_NOT_FOUND");
    }

    @Test
    @DisplayName("Geçmiş en eskiden yeniye listelenir")
    void shouldListActivity() {
        when(listActivity.listActivity(any()))
                .thenReturn(List.of(new DiscrepancyActivity(
                        UUID.randomUUID(), "OPENED", "QUANTITY_RED", null, UUID.randomUUID(), Instant.now())));

        MvcTestResult result = get(ONE + "/activity", quality());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].actionType").isEqualTo("OPENED");
        assertThat(result).bodyJson().doesNotHavePath("$[0].evidenceId");
    }

    @Test
    @DisplayName("Token'sız istek 401 alır")
    void shouldRequireAuthentication() {
        assertThat(mvc.get().uri(BASE).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private static Jwt quality() {
        return TestTokens.token("quality").build();
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
