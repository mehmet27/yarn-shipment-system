package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.in.GetAmountSummaryUseCase;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.domain.model.AmountSummary;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.SubcontractorAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tutar bilgisi: kilolar ile her satırın kendi birim fiyatından hesaplanır. */
@Service
public class AmountSummaryService implements GetAmountSummaryUseCase {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BalanceFirePort port;
    private final ReportAccess access;
    private final ListToInvoiceUseCase listToInvoice;
    private final Clock clock;

    AmountSummaryService(BalanceFirePort port, ReportAccess access, ListToInvoiceUseCase listToInvoice, Clock clock) {
        this.port = port;
        this.access = access;
        this.listToInvoice = listToInvoice;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AmountSummary getAmountSummary(ReportQuery query) {
        ReportPeriod period =
                ReportPeriod.of(query.from(), query.to(), LocalDate.ofInstant(clock.instant(), ReportPeriod.ZONE));
        var scope = access.scopeOf(query);
        if (scope.isEmpty()) {
            return new AmountSummary(ZERO, ZERO, ZERO, ZERO, List.of());
        }
        var shipped = port.findShippedAmount(period, scope.get());
        List<FireFile> toInvoice = listToInvoice.listToInvoice(query);
        Map<UUID, BigDecimal> prices = port.findAveragePrices(
                toInvoice.stream().map(FireFile::shipmentId).toList());
        BigDecimal invoiceAmount = ZERO;
        BigDecimal invoiceUnpriced = ZERO;
        for (FireFile file : toInvoice) {
            BigDecimal price = prices.get(file.shipmentId());
            if (price == null) {
                invoiceUnpriced = invoiceUnpriced.add(file.shortageKg());
            } else {
                invoiceAmount = invoiceAmount.add(file.shortageKg().multiply(price));
            }
        }
        return new AmountSummary(
                money(shipped.amount()),
                shipped.unpricedKg(),
                money(invoiceAmount),
                invoiceUnpriced,
                subcontractorAmounts(port.findPricedBalances(scope.get())));
    }

    private static List<SubcontractorAmount> subcontractorAmounts(List<BalanceFirePort.PricedBalance> balances) {
        Map<UUID, SubcontractorAmount> bySubcontractor = new LinkedHashMap<>();
        for (BalanceFirePort.PricedBalance row : balances) {
            BigDecimal amount = row.unitPrice() == null ? ZERO : row.balanceKg().multiply(row.unitPrice());
            BigDecimal unpriced = row.unitPrice() == null ? row.balanceKg() : ZERO;
            bySubcontractor.merge(
                    row.subcontractorId(),
                    new SubcontractorAmount(
                            row.subcontractorId(), row.subcontractorName(), row.balanceKg(), amount, unpriced),
                    (a, b) -> new SubcontractorAmount(
                            a.subcontractorId(),
                            a.subcontractorName(),
                            a.balanceKg().add(b.balanceKg()),
                            a.amount().add(b.amount()),
                            a.unpricedKg().add(b.unpricedKg())));
        }
        return bySubcontractor.values().stream()
                .map(a -> new SubcontractorAmount(
                        a.subcontractorId(), a.subcontractorName(), a.balanceKg(), money(a.amount()), a.unpricedKg()))
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
