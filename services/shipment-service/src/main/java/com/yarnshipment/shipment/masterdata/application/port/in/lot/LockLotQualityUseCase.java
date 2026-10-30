package com.yarnshipment.shipment.masterdata.application.port.in.lot;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Partinin kalitesini kilitler ve LotQualityLocked olayını yayınlar; kilit geri alınmaz (rapor Bölüm 10). */
public interface LockLotQualityUseCase {

    ProductionLot lockLotQuality(Actor actor, UUID id);
}
