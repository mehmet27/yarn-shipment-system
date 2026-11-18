package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementPage;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fire dosyası yanıtları. Sözleşmedeki düz alan listesi, {@code @JsonUnwrapped} ile hesap ve fatura bloklarından
 * birleştirilir.
 */
final class FireViews {

    private FireViews() {}

    record FireSettlementView(
            UUID shipmentId,
            String status,
            String fireRate,
            @JsonUnwrapped FireFiguresView figures,
            @JsonUnwrapped InvoiceMarkView invoice,
            @JsonUnwrapped FireInvoiceViews.FileInvoices fileInvoices) {}

    /** G, D, I, K, T, B, E, H ve gerçekleşen fire (analiz 8.1); faturalanan ve kalan kilo. */
    @SuppressWarnings("checkstyle:RecordComponentNumber")
    record FireFiguresView(
            String sentKg,
            String returnedKg,
            String claimKg,
            String fabricKg,
            String baseKg,
            String expectedFabricKg,
            String shortageKg,
            String faultyKg,
            String actualFirePercent,
            String invoicedKg,
            String remainingKg) {}

    record InvoiceMarkView(
            @Nullable String invoiceNo,
            @Nullable LocalDate invoicedAt,
            @Nullable UUID invoicedBy,
            @Nullable String invoiceNote) {}

    @SuppressWarnings("checkstyle:RecordComponentNumber")
    record FireSettlementRowView(
            UUID shipmentId,
            String shipmentNo,
            String subcontractorTitle,
            String status,
            String sentKg,
            String fabricKg,
            String expectedFabricKg,
            String faultyKg,
            String actualFirePercent,
            String invoicedKg,
            String remainingKg) {}

    record FireSettlementPageView(List<FireSettlementRowView> items, PageMetadataView page) {}

    static FireSettlementView fireSettlement(FireSettlementDetails details) {
        FireSettlement settlement = details.settlement();
        FireCalculation calc = details.calculation();
        return new FireSettlementView(
                settlement.shipmentId(),
                settlement.status().name(),
                settlement.fireRate().toPlainString(),
                new FireFiguresView(
                        BalanceViews.kg(calc.sentKg()),
                        BalanceViews.kg(calc.returnedKg()),
                        BalanceViews.kg(calc.claimKg()),
                        BalanceViews.kg(calc.fabricKg()),
                        BalanceViews.kg(calc.baseKg()),
                        BalanceViews.kg(calc.expectedFabricKg()),
                        BalanceViews.kg(calc.shortageKg()),
                        BalanceViews.kg(calc.faultyKg()),
                        calc.actualFirePercent().toPlainString(),
                        BalanceViews.kg(calc.invoicedKg()),
                        BalanceViews.kg(calc.remainingKg())),
                new InvoiceMarkView(
                        settlement.invoiceNo(),
                        settlement.invoicedOn(),
                        settlement.invoicedBy(),
                        settlement.invoiceNote()),
                FireInvoiceViews.of(details));
    }

    static FireSettlementPageView fireSettlements(FireSettlementPage page) {
        return new FireSettlementPageView(
                page.items().stream().map(FireViews::row).toList(),
                new PageMetadataView(page.number(), page.size(), page.totalElements(), page.totalPages()));
    }

    private static FireSettlementRowView row(FireSettlementPage.Row row) {
        FireCalculation calc = row.calculation();
        return new FireSettlementRowView(
                row.shipmentId(),
                row.shipmentNo(),
                row.subcontractorTitle(),
                row.status().name(),
                BalanceViews.kg(calc.sentKg()),
                BalanceViews.kg(calc.fabricKg()),
                BalanceViews.kg(calc.expectedFabricKg()),
                BalanceViews.kg(calc.faultyKg()),
                calc.actualFirePercent().toPlainString(),
                BalanceViews.kg(calc.invoicedKg()),
                BalanceViews.kg(calc.remainingKg()));
    }
}
