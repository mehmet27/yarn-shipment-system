package com.yarnshipment.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.GetDashboardUseCase;
import com.yarnshipment.reporting.application.port.in.ListShipmentReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorReportUseCase;
import com.yarnshipment.reporting.domain.model.ShipmentComparison;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.math.BigDecimal;
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

/** Sevkiyat dökümü ve fasoncu bakiye uçlarının sözleşmesi ve izin denetimi. */
@WebMvcTest(controllers = ReportController.class)
class ReportShipmentControllerTest {

    private static final String BASE = "/api/v1/reports";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetDashboardUseCase getDashboard;

    @MockitoBean
    private ListSubcontractorReportUseCase listSubcontractors;

    @MockitoBean
    private ListShipmentReportUseCase listShipments;

    @MockitoBean
    private ListSubcontractorBalanceUseCase listBalances;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ReportShipmentControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Sevkiyat dökümü kilo ve yüzdeleri dize, olmayan alanları atlanmış döner")
    void shouldReturnShipmentReport() {
        UUID id = UUID.randomUUID();
        when(listShipments.listShipmentReport(any()))
                .thenReturn(List.of(new ShipmentRow(
                        id,
                        "SHP-1",
                        UUID.randomUUID(),
                        "COMPLETED",
                        new ShipmentComparison(
                                new BigDecimal("1000.000"),
                                new BigDecimal("990.000"),
                                new BigDecimal("10.000"),
                                new BigDecimal("1.00"),
                                "GREEN"),
                        false,
                        Instant.parse("2026-11-02T09:00:00Z"),
                        null)));

        MvcTestResult result = get(BASE + "/shipments?from=2026-11-01&to=2026-11-03", manager());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].shipmentNo").isEqualTo("SHP-1");
        assertThat(result).bodyJson().extractingPath("$[0].diffKg").isEqualTo("10.000");
        assertThat(result).bodyJson().extractingPath("$[0].band").isEqualTo("GREEN");
        assertThat(result).bodyJson().extractingPath("$[0].hadDiscrepancy").isEqualTo(false);
        assertThat(result).bodyJson().doesNotHavePath("$[0].driverId");
    }

    @Test
    @DisplayName("Fasoncu bakiyesi çıkış, teslim, yolda kalan ve farkı döner; izinsiz kullanıcı 403 alır")
    void shouldReturnSubcontractorBalance() {
        when(listBalances.listSubcontractorBalance(any()))
                .thenReturn(List.of(new SubcontractorBalance(
                        UUID.randomUUID(),
                        2,
                        new BigDecimal("1500.000"),
                        new BigDecimal("990.000"),
                        new BigDecimal("500.000"))));

        MvcTestResult result = get(BASE + "/subcontractor-balance", manager());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].inTransitKg").isEqualTo("500.000");
        assertThat(result).bodyJson().extractingPath("$[0].differenceKg").isEqualTo("10.000");
        assertThat(get(
                        BASE + "/subcontractor-balance",
                        TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(get(BASE + "/shipments", TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.FORBIDDEN);
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
