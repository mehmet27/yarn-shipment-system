package com.yarnshipment.shipment.application.port.in.fabric;

import java.util.List;

/** Sevkiyatın ipliğinden üretilen kumaşları (başka sevkiyatta girilenler dahil) eskiden yeniye okur. */
public interface ListFabricDrawsUseCase {

    List<FabricDrawEntry> listFabricDraws(ShipmentRecordsQuery query);
}
