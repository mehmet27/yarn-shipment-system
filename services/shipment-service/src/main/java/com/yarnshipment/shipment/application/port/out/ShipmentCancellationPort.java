package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.cancellation.Cancellation;
import java.util.UUID;

/** İptali yazar: durum geçişi, gerekçe, geçmiş satırı, olay yayını ve sevkiyattaki çuvalların çıkarılması. */
public interface ShipmentCancellationPort {

    /** Arada durum değiştiyse {@code InvalidShipmentStatusException}. */
    void cancel(Cancellation cancellation, UUID actorUserId);
}
