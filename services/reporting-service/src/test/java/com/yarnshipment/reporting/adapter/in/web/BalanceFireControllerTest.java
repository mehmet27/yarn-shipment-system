package com.yarnshipment.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ImportOpeningBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListBalanceMatrixUseCase;
import com.yarnshipment.reporting.application.port.in.ListFireReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ListYarnVsFabricUseCase;
import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningImportResult;
import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Bakiye matrisi, fire raporları ve açılış bakiyesi uçlarının sözleşmesi. */
@WebMvcTest(controllers = BalanceFireController.class)
class BalanceFireControllerTest {

    private static final String BASE = "/api/v1/reports";

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
    private ImportOpeningBalanceUseCase importOpening;

    @Autowired
    BalanceFireControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Bakiye matrisi kiloları ondalık dize döner")
    void shouldReturnBalanceMatrix() {
        UUID sub = UUID.randomUUID();
        when(listBalanceMatrix.listBalanceMatrix(any()))
                .thenReturn(List.of(new BalanceMatrixRow(
                        "Viskon", sub, "Kırıkhan", new BigDecimal("600.000"), new BigDecimal("40.000"))));

        MvcTestResult result = get(BASE + "/balance-matrix");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].balanceKg").isEqualTo("600.000");
        assertThat(result).bodyJson().extractingPath("$[0].inTransitKg").isEqualTo("40.000");
    }

    @Test
    @DisplayName("Fire raporu, faturalanacaklar ve iplik-kumaş satırları döner")
    void shouldReturnFireReports() {
        FireFile file = new FireFile(
                UUID.randomUUID(),
                "SHP-1",
                UUID.randomUUID(),
                "Kırıkhan",
                new BigDecimal("34032.000"),
                BigDecimal.ZERO,
                new BigDecimal("32660.000"),
                new BigDecimal("0.04"),
                "TO_INVOICE",
                null,
                null,
                BigDecimal.ZERO);
        when(listFire.listFire(any(), any())).thenReturn(List.of(file));
        when(listToInvoice.listToInvoice(any())).thenReturn(List.of(file));
        when(listYarnVsFabric.listYarnVsFabric(any())).thenReturn(YarnVsFabricRow.aggregate(List.of(file)));

        assertThat(get(BASE + "/fire"))
                .bodyJson()
                .extractingPath("$[0].shortageKg")
                .isEqualTo("10.720");
        assertThat(get(BASE + "/fire"))
                .bodyJson()
                .extractingPath("$[0].actualFireRate")
                .isEqualTo("0.0403");
        assertThat(get(BASE + "/to-invoice"))
                .bodyJson()
                .extractingPath("$[0].status")
                .isEqualTo("TO_INVOICE");
        assertThat(get(BASE + "/yarn-vs-fabric"))
                .bodyJson()
                .extractingPath("$[0].fabricKg")
                .isEqualTo("32660.000");
    }

    @Test
    @DisplayName("Açılış bakiyesi içe aktarılan ve atlanan satırları döner")
    void shouldImportOpeningBalance() {
        when(importOpening.importOpening(any(), any(), any())).thenReturn(new OpeningImportResult(1, 0));
        String row = "{\"subcontractorId\":\"" + UUID.randomUUID() + "\",\"subcontractorName\":\"A\","
                + "\"yarnType\":\"Viskon\",\"netKg\":\"10.500\"}";
        String body = "{\"asOf\":\"2026-11-09\",\"rows\":[" + row + "]}";

        MvcTestResult result = mvc.post()
                .uri(BASE + "/balance/opening-import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(manager())))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.imported").isEqualTo(1);
    }

    private static Jwt manager() {
        return TestTokens.token("manager").build();
    }

    private MvcTestResult get(String uri) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(manager())))
                .exchange();
    }
}
