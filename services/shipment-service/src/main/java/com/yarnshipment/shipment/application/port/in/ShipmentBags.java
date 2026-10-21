package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Çuval görünümü. Görünüme ait olmayan liste {@code null} kalır; boş liste "henüz yok" demektir.
 *
 * @param audience görünüm türü
 * @param dispatchBags çıkışta atananlar; fasoncu ve şoför görünümünde yok
 * @param deliveryScans teslimde okutulanlar; depo ve şoför görünümünde yok
 */
public record ShipmentBags(
        ShipmentAudience audience,
        @Nullable List<DispatchBag> dispatchBags,
        @Nullable List<DeliveryScan> deliveryScans) {

    public ShipmentBags {
        Objects.requireNonNull(audience, "audience");
    }
}
