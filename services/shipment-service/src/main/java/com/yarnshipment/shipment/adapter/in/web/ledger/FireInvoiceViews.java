package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import java.time.LocalDate;
import java.util.List;

/** Fire dosyası yanıtındaki fatura blokları: faturalanabilir iplikler ve girilmiş faturalar. */
final class FireInvoiceViews {

    private FireInvoiceViews() {}

    record FileInvoices(List<YarnKgView> yarns, List<InvoiceEntryView> invoices) {}

    /** Sevkiyatın bir iplik cinsi ve gönderilen kilosu (fatura satırı seçeneği). */
    record YarnKgView(String yarnType, String dispatchedKg) {}

    /** Girilmiş bir fatura. */
    record InvoiceEntryView(String invoiceNo, LocalDate invoicedOn, String kg) {}

    static FileInvoices of(FireSettlementDetails details) {
        return new FileInvoices(
                details.yarns().entrySet().stream()
                        .map(entry -> new YarnKgView(entry.getKey(), BalanceViews.kg(entry.getValue())))
                        .toList(),
                details.invoices().stream()
                        .map(entry -> new InvoiceEntryView(
                                entry.invoiceNo(), entry.invoicedOn(), BalanceViews.kg(entry.kg())))
                        .toList());
    }
}
