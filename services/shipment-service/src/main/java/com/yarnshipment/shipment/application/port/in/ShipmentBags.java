package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.DispatchBag;
import java.util.List;
import java.util.Objects;

/**
 * Çuval görünümü; boş liste "henüz yok" demektir.
 *
 * @param audience görünüm türü
 * @param dispatchBags çıkışta atananlar
 */
public record ShipmentBags(ShipmentAudience audience, List<DispatchBag> dispatchBags) {

    public ShipmentBags {
        Objects.requireNonNull(audience, "audience");
        dispatchBags = List.copyOf(dispatchBags);
    }
}
