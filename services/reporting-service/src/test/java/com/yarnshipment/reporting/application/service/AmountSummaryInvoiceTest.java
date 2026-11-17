package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort.ShippedAmount;
import com.yarnshipment.reporting.domain.model.AmountSummary;
import com.yarnshipment.reporting.domain.model.FireFile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Faturalanacak tutar: her dosya kendi ortalama birim fiyatıyla hesaplanır. */
class AmountSummaryInvoiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    private final BalanceFirePort port = mock(BalanceFirePort.class);
    private final ListToInvoiceUseCase toInvoice = mock(ListToInvoiceUseCase.class);
    private final AmountSummaryService service = new AmountSummaryService(
            port, new ReportAccess(new ScopeGuard(mock(AuditPublisher.class), CLOCK)), toInvoice);

    @Test
    @DisplayName("Faturalanacak tutar her dosyanın kendi ortalama fiyatıyla hesaplanır; fiyatsız dosya ayrı yazılır")
    void shouldPriceEachInvoiceFileWithItsOwnPrice() {
        UUID priced = UUID.randomUUID();
        UUID unpriced = UUID.randomUUID();
        when(toInvoice.listToInvoice(any())).thenReturn(List.of(file(priced), file(unpriced)));
        when(port.findShippedAmount(any())).thenReturn(new ShippedAmount(new BigDecimal("1000"), BigDecimal.TEN));
        when(port.findAveragePrices(any())).thenReturn(Map.of(priced, new BigDecimal("20.5")));
        when(port.findPricedBalances(any())).thenReturn(List.of());

        AmountSummary summary = service.getAmountSummary(query());

        // eksik kilo 60 kg × 20,5 TL
        assertThat(summary.toInvoiceAmount()).isEqualByComparingTo("1230.00");
        assertThat(summary.toInvoiceUnpricedKg()).isEqualByComparingTo("60.000");
        assertThat(summary.shippedAmount()).isEqualByComparingTo("1000.00");
        assertThat(summary.shippedUnpricedKg()).isEqualByComparingTo("10");
    }

    private static ReportQuery query() {
        return new ReportQuery(TestTokens.actor("manager"), null, null, null);
    }

    private static FireFile file(UUID shipmentId) {
        return new FireFile(
                shipmentId,
                "SHP",
                UUID.randomUUID(),
                "K",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("900"),
                new BigDecimal("0.04"),
                "TO_INVOICE",
                null,
                null,
                BigDecimal.ZERO);
    }
}
