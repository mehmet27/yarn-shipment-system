package com.yarnshipment.shipment.application.port.out.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Tamamlanan sevkiyatı bakiye defterine yazar. */
public interface DispatchLedgerPort {

    /**
     * Sevkiyat satırı başına (satır yoksa çıkış tartısı neti tek satır) DISPATCH hareketi yazar. Aynı sevkiyat için
     * tekrar çağrılırsa yeni hareket üretmez.
     *
     * @return gönderilen toplam net kg
     */
    BigDecimal recordDispatch(UUID shipmentId, UUID subcontractorId, Instant at, UUID createdBy);
}
