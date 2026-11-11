package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import java.util.List;
import java.util.UUID;

/** Kumaş girişi portu; kayıt ve bakiyeden düşen hareketler aynı transaction'da yazılır. */
public interface FabricReceiptPort {

    /** Sevkiyatın girişleri, eskiden yeniye. */
    List<FabricReceipt> findByShipment(UUID shipmentId);

    void save(FabricReceipt receipt);
}
