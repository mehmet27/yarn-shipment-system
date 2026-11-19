package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.application.port.in.fire.FireInvoicePage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Faturalananlar listesi yanıtı. */
final class FireInvoiceListViews {

    private FireInvoiceListViews() {}

    record InvoiceRowView(
            String invoiceNo,
            LocalDate invoicedOn,
            UUID shipmentId,
            String shipmentNo,
            UUID subcontractorId,
            String subcontractorTitle,
            String kg) {}

    record InvoicePageView(List<InvoiceRowView> items, PageMetadataView page, String totalKg) {}

    static InvoicePageView of(FireInvoicePage page) {
        return new InvoicePageView(
                page.items().stream()
                        .map(row -> new InvoiceRowView(
                                row.invoiceNo(),
                                row.invoicedOn(),
                                row.shipmentId(),
                                row.shipmentNo(),
                                row.subcontractorId(),
                                row.subcontractorTitle(),
                                BalanceViews.kg(row.kg())))
                        .toList(),
                new PageMetadataView(page.number(), page.size(), page.totalElements(), page.totalPages()),
                BalanceViews.kg(page.totalKg()));
    }
}
