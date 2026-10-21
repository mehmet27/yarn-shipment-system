package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import java.util.List;
import java.util.UUID;

/** Çuval okuma portu. */
public interface ShipmentBagsPort {

    /** Çıkışta atanan, sonradan çıkarılmamış çuvallar; atama sırasıyla. */
    List<DispatchBag> findDispatchBags(UUID shipmentId);

    /** Teslim kaydındaki okutmalar; okutma sırasıyla. Teslim kaydı yoksa boş. */
    List<DeliveryScan> findDeliveryScans(UUID shipmentId);
}
