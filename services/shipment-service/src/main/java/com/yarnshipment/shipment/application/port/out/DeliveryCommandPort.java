package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.DeliveryReceipt;
import java.util.UUID;

/** Teslim tutanağı yazma portu; tutanak, okutmalar, tartı, mühür, durum geçişi ve olay tek transaction'dadır. */
public interface DeliveryCommandPort {

    /** Sevkiyat için tutanak daha önce gönderildi mi (yeniden deneme). */
    boolean hasDeliveryReceipt(UUID shipmentId);

    /** Arada durum değiştiyse {@code InvalidShipmentStatusException}. */
    void recordDelivery(DeliveryReceipt receipt);
}
