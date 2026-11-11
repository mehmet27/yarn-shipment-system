package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.util.List;

/** Sevkiyatın iplik dönüşü ve fasoncudan gelen iplik kayıtlarını eskiden yeniye okur. */
public interface ListYarnReturnsUseCase {

    List<YarnReturn> listYarnReturns(ShipmentRecordsQuery query);
}
