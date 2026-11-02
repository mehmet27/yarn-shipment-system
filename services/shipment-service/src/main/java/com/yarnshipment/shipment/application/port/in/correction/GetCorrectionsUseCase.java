package com.yarnshipment.shipment.application.port.in.correction;

import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import java.util.List;

/** Sevkiyatın düzeltme kayıtları, eskiden yeniye. */
public interface GetCorrectionsUseCase {

    List<ShipmentCorrection> getCorrections(GetShipmentHistoryQuery query);
}
