package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort.PricedBalance;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort.ShippedAmount;
import com.yarnshipment.reporting.domain.model.AmountSummary;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tutar bilgisi: her satır kendi fiyatıyla hesaplanır, fiyatsız kilo ayrıca verilir. */
class AmountSummaryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    private final BalanceFirePort port = mock(BalanceFirePort.class);
    private final ListToInvoiceUseCase toInvoice = mock(ListToInvoiceUseCase.class);
    private final AmountSummaryService service = new AmountSummaryService(
            port, new ReportAccess(new ScopeGuard(mock(AuditPublisher.class), CLOCK)), toInvoice, CLOCK);

    @Test
    @DisplayName("Lokasyon kapsamı olmayan kullanıcı sıfır tutar görür, sorgu çalışmaz")
    void shouldReturnZeroForUserWithoutSites() {
        AmountSummary summary =
                service.getAmountSummary(new ReportQuery(TestTokens.actor(TestTokens.noSiteToken()), null, null, null));

        assertThat(summary.shippedAmount()).isEqualByComparingTo("0");
        assertThat(summary.subcontractors()).isEmpty();
        verifyNoInteractions(port);
    }

    @Test
    @DisplayName("Fasoncudaki iplik değeri fasoncu başına toplanır; fiyatı olmayan iplik kilosu ayrı yazılır")
    void shouldSumSubcontractorYarnValue() {
        UUID subA = UUID.randomUUID();
        UUID subB = UUID.randomUUID();
        when(toInvoice.listToInvoice(any())).thenReturn(List.of());
        when(port.findShippedAmount(any(), any())).thenReturn(new ShippedAmount(BigDecimal.ZERO, BigDecimal.ZERO));
        when(port.findPricedBalances(any()))
                .thenReturn(List.of(
                        new PricedBalance(subA, "A", new BigDecimal("100"), new BigDecimal("10")),
                        new PricedBalance(subA, "A", new BigDecimal("50"), new BigDecimal("20")),
                        new PricedBalance(subB, "B", new BigDecimal("30"), null)));

        AmountSummary summary = service.getAmountSummary(query());

        assertThat(summary.subcontractors()).hasSize(2);
        assertThat(summary.subcontractors().getFirst().subcontractorName()).isEqualTo("A");
        assertThat(summary.subcontractors().getFirst().balanceKg()).isEqualByComparingTo("150");
        assertThat(summary.subcontractors().getFirst().amount()).isEqualByComparingTo("2000.00");
        assertThat(summary.subcontractorYarnAmount()).isEqualByComparingTo("2000.00");
        assertThat(summary.subcontractorUnpricedKg()).isEqualByComparingTo("30");
    }

    private static ReportQuery query() {
        return new ReportQuery(TestTokens.actor("manager"), null, null, null);
    }
}
