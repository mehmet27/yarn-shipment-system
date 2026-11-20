package com.yarnshipment.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.GetAmountSummaryUseCase;
import com.yarnshipment.reporting.application.port.in.ImportOpeningBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListBalanceMatrixUseCase;
import com.yarnshipment.reporting.application.port.in.ListFireReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ListYarnVsFabricUseCase;
import com.yarnshipment.reporting.domain.model.OpeningImportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Açılış bakiyesi deftere hareket yazar: yalnızca müdür yükleyebilir. Bakiyeyi görebilen (BALANCE_VIEW) kalite ve genel
 * müdür ile diğer roller isteği doğrudan gönderse de reddedilir.
 */
@WebMvcTest(controllers = BalanceFireController.class)
class OpeningImportAccessTest {

    private static final String BODY = "{\"asOf\":\"2026-11-09\",\"rows\":[]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private ListBalanceMatrixUseCase listBalanceMatrix;

    @MockitoBean
    private ListFireReportUseCase listFire;

    @MockitoBean
    private ListYarnVsFabricUseCase listYarnVsFabric;

    @MockitoBean
    private ListToInvoiceUseCase listToInvoice;

    @MockitoBean
    private GetAmountSummaryUseCase getAmountSummary;

    @MockitoBean
    private ImportOpeningBalanceUseCase importOpening;

    @Autowired
    OpeningImportAccessTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Müdür açılış bakiyesi yükleyebilir")
    void shouldAllowManager() {
        when(importOpening.importOpening(any(), any(), any())).thenReturn(new OpeningImportResult(0, 0));

        assertThat(importAs("manager")).hasStatusOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {"quality", "general_manager", "warehouse", "admin"})
    @DisplayName("Müdür dışındaki roller, bakiyeyi görebilse bile açılış bakiyesi yükleyemez: 403")
    void shouldRejectOtherRoles(String role) {
        assertThat(importAs(role)).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(importOpening);
    }

    private MvcTestResult importAs(String role) {
        return mvc.post()
                .uri("/api/v1/reports/balance/opening-import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
