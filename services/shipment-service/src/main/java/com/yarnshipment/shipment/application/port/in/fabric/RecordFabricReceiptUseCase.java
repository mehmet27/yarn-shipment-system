package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;

/** Gelen kumaşı sevkiyata kaydeder (FABRIC_RECEIPT_RECORD); bakiyeden düşer, fire hesabını günceller. */
public interface RecordFabricReceiptUseCase {

    FabricReceipt recordFabricReceipt(RecordFabricReceiptCommand command);
}
