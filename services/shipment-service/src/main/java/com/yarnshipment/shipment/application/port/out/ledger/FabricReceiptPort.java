package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import java.util.List;
import java.util.UUID;

/** Kumaş girişi portu; kayıt ve bakiyeden düşen hareketler aynı transaction'da yazılır. */
public interface FabricReceiptPort {

    /** Sevkiyatın girişleri, eskiden yeniye. */
    List<FabricReceipt> findByShipment(UUID shipmentId);

    /** Kaydı, iplik düşümlerini ve bakiyeden düşen hareketleri aynı transaction'da yazar. */
    void save(FabricReceipt receipt);

    /** Sevkiyatın ipliğinden üretilen kumaşlar (başka sevkiyatta girilenler dahil), eskiden yeniye. */
    List<FabricDrawEntry> findDrawsFrom(UUID sourceShipmentId);
}
