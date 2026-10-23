package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.bag.ReservedBag;
import java.util.List;
import java.util.UUID;

/** Çuval servisinde çuvalları sevkiyata ayırır; hepsi ya da hiçbiri, aynı sevkiyata ayrılmış çuval hata vermez. */
public interface BagReservationPort {

    List<ReservedBag> reserve(UUID shipmentId, List<String> uniqueKeys);

    /** Sevkiyatın tüm rezervasyonlarını bırakır; bırakılacak yoksa sessizce geçer. */
    void release(UUID shipmentId, String reason);
}
