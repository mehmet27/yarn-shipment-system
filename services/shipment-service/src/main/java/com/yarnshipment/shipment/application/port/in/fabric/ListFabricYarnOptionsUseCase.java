package com.yarnshipment.shipment.application.port.in.fabric;

import java.util.List;

/** Kumaş girerken seçilebilecek, fasoncunun elindeki tüm iplikleri (tüm açık sevkiyatlarından) listeler. */
public interface ListFabricYarnOptionsUseCase {

    List<FabricYarnOption> listFabricYarnOptions(ShipmentRecordsQuery query);
}
