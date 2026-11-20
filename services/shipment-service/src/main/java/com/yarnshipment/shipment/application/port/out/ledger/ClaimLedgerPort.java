package com.yarnshipment.shipment.application.port.out.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Haklı itirazı fire dosyasına faturalanacak kilo olarak yazar. */
public interface ClaimLedgerPort {

    /**
     * Haklı itirazın kilosunu bakiyeden düşmeden fire dosyasına faturalanacak kilo olarak yazar; aynı uyuşmazlık
     * tekrar gelirse dokunmaz.
     *
     * @return kayıt şimdi yazıldıysa {@code true}; aynı uyuşmazlık daha önce yazılmışsa {@code false}
     */
    boolean recordInvoiceableClaim(InvoiceableClaim claim);

    /**
     * Faturayla bakiyeden düşülecek haklı itiraz.
     *
     * @param discrepancyId itirazın uyuşmazlığı; aynı uyuşmazlık bir kez yazılır
     * @param shipmentId sevkiyat
     * @param claimedKg itiraz edilen kilo
     * @param decidedAt karar zamanı
     * @param decidedBy kararı veren
     */
    record InvoiceableClaim(
            UUID discrepancyId, UUID shipmentId, BigDecimal claimedKg, Instant decidedAt, UUID decidedBy) {}
}
