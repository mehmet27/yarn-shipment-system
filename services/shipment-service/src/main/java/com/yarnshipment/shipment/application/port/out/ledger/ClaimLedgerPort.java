package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Haklı itirazın bakiye hareketlerini defterine yazar. */
public interface ClaimLedgerPort {

    /** Sevkiyatın fasoncusu; sevkiyat yoksa ya da fasoncusuz ise boş. */
    Optional<UUID> findSubcontractor(UUID shipmentId);

    /**
     * Haklı itirazın kilosunu bakiyeden düşmeden fire dosyasına faturalanacak kilo olarak yazar; aynı uyuşmazlık
     * tekrar gelirse dokunmaz.
     */
    void recordInvoiceableClaim(InvoiceableClaim claim);

    /** Hareketi ekler; aynı kaynak daha önce yazılmışsa dokunmaz. */
    void append(BalanceMovement movement);

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
