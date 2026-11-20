package com.yarnshipment.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ListMonthlyTrendUseCase;
import com.yarnshipment.reporting.domain.model.MonthlyFlow;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Aylık eğilim ucunun sözleşmesi. */
@WebMvcTest(controllers = MonthlyTrendController.class)
class MonthlyTrendControllerTest {

    private static final String URI = "/api/v1/reports/monthly-trend";

    private static final int SHIPMENTS = 4;
    private static final int THREE_MONTHS = 3;

    private final MockMvcTester mvc;

    @MockitoBean
    private ListMonthlyTrendUseCase listMonthlyTrend;

    @Autowired
    MonthlyTrendControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Ay sayısı verilmezse 6 ay istenir; kilolar ondalık dize, ay YYYY-MM döner")
    void shouldReturnMonthsWithDefaultCount() {
        when(listMonthlyTrend.listMonthlyTrend(any(), eq(ListMonthlyTrendUseCase.DEFAULT_MONTHS)))
                .thenReturn(List.of(new MonthlyFlow(
                        YearMonth.parse("2026-11"),
                        SHIPMENTS,
                        new BigDecimal("34032.000"),
                        new BigDecimal("0.000"),
                        new BigDecimal("32660.000"),
                        new BigDecimal("0.0403"),
                        new BigDecimal("10.720"))));

        MvcTestResult result = get(URI, "manager");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].month").isEqualTo("2026-11");
        assertThat(result).bodyJson().extractingPath("$[0].from").isEqualTo("2026-11-01");
        assertThat(result).bodyJson().extractingPath("$[0].to").isEqualTo("2026-11-30");
        assertThat(result).bodyJson().extractingPath("$[0].sentKg").isEqualTo("34032.000");
        assertThat(result).bodyJson().extractingPath("$[0].fabricKg").isEqualTo("32660.000");
        assertThat(result).bodyJson().extractingPath("$[0].actualFireRate").isEqualTo("0.0403");
        assertThat(result).bodyJson().extractingPath("$[0].shipmentCount").isEqualTo(SHIPMENTS);
    }

    @Test
    @DisplayName("İstenen ay sayısı servise aynen geçer")
    void shouldPassRequestedMonthCount() {
        when(listMonthlyTrend.listMonthlyTrend(any(), eq(THREE_MONTHS))).thenReturn(List.of());

        assertThat(get(URI + "?months=3&to=2026-10-05", "manager")).hasStatusOk();
    }

    @Test
    @DisplayName("Rapor izni olmayan (depo) eğilimi göremez: 403")
    void shouldForbidWithoutReportPermission() {
        assertThat(get(URI, "warehouse")).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult get(String uri, String role) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }
}
