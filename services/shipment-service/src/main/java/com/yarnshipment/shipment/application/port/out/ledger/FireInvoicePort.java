package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.fire.InvoiceEntry;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.InvoiceShare;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Fire dosyasına girilen faturalar portu; faturalanan kilo fasoncunun iplik bakiyesinden düşer. */
public interface FireInvoicePort {

    /** Sevkiyatta iplik cinsine göre gönderilen kg (fatura satırlarında seçilecek iplikler). */
    Map<String, BigDecimal> dispatchedByYarn(UUID shipmentId);

    /** Bu fatura numarası bu sevkiyata daha önce girildi mi. */
    boolean invoiceExists(UUID shipmentId, String invoiceNo);

    /** Faturayı ve iplik başına kilosunu yazar; kilolar fasoncunun bakiyesinden düşer. */
    void recordInvoice(InvoiceRecord invoice);

    /** Sevkiyata girilen faturalar, eskisi önce. */
    List<InvoiceEntry> invoices(UUID shipmentId);

    /** Yazılacak fatura: iplik başına kiloları ve fasoncu bakiyesine yazılacak bağlam. */
    record InvoiceRecord(
            UUID shipmentId,
            UUID subcontractorId,
            String invoiceNo,
            LocalDate invoicedOn,
            @Nullable String note,
            List<InvoiceShare> lines,
            UUID invoicedBy,
            Instant at) {}
}
