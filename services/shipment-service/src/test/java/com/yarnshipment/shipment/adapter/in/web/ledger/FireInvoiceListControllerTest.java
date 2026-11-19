package com.yarnshipment.shipment.adapter.in.web.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fire.FireInvoicePage;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Faturalananlar listesi ucunun sözleşmesi: izin, süzgeçler, düz JSON biçimi ve tarih aralığı doğrulaması. */
@WebMvcTest(controllers = FireInvoiceListController.class)
class FireInvoiceListControllerTest {

    private static final int PAGE_SIZE = 20;
    private static final String URI = "/api/v1/fire-invoices";
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000901");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000902");

    private final MockMvcTester mvc;

    @MockitoBean
    private ListFireInvoicesUseCase listFireInvoicesUseCase;

    @Autowired
    FireInvoiceListControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    private MvcTestResult get(String role, String uri) {
        return mvc.get()
                .uri(uri)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }

    @Test
    @DisplayName("Faturalar düz alanlarla döner; kilolar 3 ondalıklı dize, toplam kilo süzgeçteki tüm faturaların")
    void shouldReturnInvoices() {
        // given
        BigDecimal kg = new BigDecimal("20");
        FireInvoicePage.Row row = new FireInvoicePage.Row(
                "F-7", LocalDate.parse("2026-11-12"), SHIPMENT, "SV-1", SUBCONTRACTOR, "Örnek Tekstil", kg);
        when(listFireInvoicesUseCase.listFireInvoices(any()))
                .thenReturn(new FireInvoicePage(List.of(row), 0, PAGE_SIZE, 1, kg));

        // when
        MvcTestResult result = get("manager", URI + "?from=2026-11-01&to=2026-11-30&subcontractorId=" + SUBCONTRACTOR);

        // then
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items[0].invoiceNo").isEqualTo("F-7");
        assertThat(result).bodyJson().extractingPath("$.items[0].invoicedOn").isEqualTo("2026-11-12");
        assertThat(result).bodyJson().extractingPath("$.items[0].shipmentNo").isEqualTo("SV-1");
        assertThat(result).bodyJson().extractingPath("$.items[0].kg").isEqualTo("20.000");
        assertThat(result).bodyJson().extractingPath("$.totalKg").isEqualTo("20.000");
        assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        ArgumentCaptor<ListFireInvoicesQuery> query = ArgumentCaptor.forClass(ListFireInvoicesQuery.class);
        verify(listFireInvoicesUseCase).listFireInvoices(query.capture());
        assertThat(query.getValue().from()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(query.getValue().to()).isEqualTo(LocalDate.parse("2026-11-30"));
        assertThat(query.getValue().subcontractorId()).isEqualTo(SUBCONTRACTOR);
    }

    @Test
    @DisplayName("BALANCE_VIEW'siz rol 403 alır; başlangıcı bitişten sonra olan aralık 400 alır")
    void shouldGuardAndValidateRange() {
        assertThat(get("warehouse", URI)).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult reversed = get("manager", URI + "?from=2026-11-30&to=2026-11-01");

        assertThat(reversed).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(reversed).bodyJson().extractingPath("$.code").isEqualTo("INVALID_LEDGER_INPUT");
        verifyNoInteractions(listFireInvoicesUseCase);
    }
}
