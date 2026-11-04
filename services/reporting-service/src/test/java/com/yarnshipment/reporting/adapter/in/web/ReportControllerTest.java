package com.yarnshipment.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.GetDashboardUseCase;
import com.yarnshipment.reporting.application.port.in.ListDriverReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListPairReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorReportUseCase;
import com.yarnshipment.reporting.domain.model.InvalidReportPeriodException;
import java.util.List;
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

/** Rapor uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = ReportController.class)
class ReportControllerTest {

    private static final String BASE = "/api/v1/reports";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetDashboardUseCase getDashboard;

    @MockitoBean
    private ListDriverReportUseCase listDrivers;

    @MockitoBean
    private ListSubcontractorReportUseCase listSubcontractors;

    @MockitoBean
    private ListPairReportUseCase listPairs;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ReportControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Dashboard kilo ve yüzdeleri ondalık dize olarak döner")
    void shouldReturnDashboard() {
        when(getDashboard.getDashboard(any())).thenReturn(TestDashboards.sample());

        MvcTestResult result = get(BASE + "/dashboard?from=2026-11-01&to=2026-11-03", manager());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.shipments.total").isEqualTo(TestDashboards.TOTAL);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.quantity.absoluteDiffKg")
                .isEqualTo("40.000");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.largestDifferences[0].band")
                .isEqualTo("RED");
        assertThat(result).bodyJson().extractingPath("$.discrepancies.overdue").isEqualTo(1);
    }

    @Test
    @DisplayName("Şoför raporu oranı ve ortalamayı hesaplanmış döner")
    void shouldReturnDriverReport() {
        when(listDrivers.listDriverReport(any())).thenReturn(List.of(TestFigures.driver()));

        MvcTestResult result = get(BASE + "/drivers", manager());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].discrepancyRate").isEqualTo("25.00");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0].averageAbsoluteDiffKg")
                .isEqualTo("2.500");
    }

    @Test
    @DisplayName("Geçersiz dönem REPORT_PERIOD_INVALID koduyla 400 döner")
    void shouldRejectInvalidPeriod() {
        when(getDashboard.getDashboard(any())).thenThrow(new InvalidReportPeriodException("bad"));

        MvcTestResult result = get(BASE + "/dashboard", manager());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("REPORT_PERIOD_INVALID");
    }

    @Test
    @DisplayName("Rapor izni olmayan depo personeli 403 alır, tokensız istek 401")
    void shouldDenyWithoutReportPermission() {
        assertThat(get(BASE + "/dashboard", TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri(BASE + "/dashboard").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Fasoncu ve eşleşme raporları satırları döner; vadesi geçenler dashboard'da listelenir")
    void shouldReturnSubcontractorAndPairReports() {
        when(listSubcontractors.listSubcontractorReport(any())).thenReturn(List.of(TestFigures.driver()));
        when(listPairs.listPairReport(any())).thenReturn(List.of(TestFigures.pair()));
        when(getDashboard.getDashboard(any())).thenReturn(TestDashboards.withOverdue());

        assertThat(get(BASE + "/subcontractors", manager()))
                .bodyJson()
                .extractingPath("$[0].shipmentCount")
                .isEqualTo(TestFigures.DRIVER_SHIPMENTS);
        assertThat(get(BASE + "/pairs", manager()))
                .bodyJson()
                .extractingPath("$[0].discrepancyCount")
                .isEqualTo(1);
        assertThat(get(BASE + "/dashboard", manager()))
                .bodyJson()
                .extractingPath("$.overdueDiscrepancies[0].severity")
                .isEqualTo("RED");
    }

    private static Jwt manager() {
        return TestTokens.token("manager").build();
    }

    private MvcTestResult get(String uri, Jwt jwt) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
