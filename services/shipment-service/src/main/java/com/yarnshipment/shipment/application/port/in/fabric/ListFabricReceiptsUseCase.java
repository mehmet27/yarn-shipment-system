package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import java.util.List;

/** Sevkiyatın kumaş girişlerini eskiden yeniye okur. */
public interface ListFabricReceiptsUseCase {

    List<FabricReceipt> listFabricReceipts(ShipmentRecordsQuery query);
}
