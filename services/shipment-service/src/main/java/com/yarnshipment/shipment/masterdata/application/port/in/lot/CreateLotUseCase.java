package com.yarnshipment.shipment.masterdata.application.port.in.lot;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Parti ekler. LOT_QUALITY_LOCK yetkisi olan kalite kullanıcısı veya yönetici, kendi lokasyonunda. */
public interface CreateLotUseCase {

    ProductionLot createLot(Actor actor, UUID siteId, LotDetails details);
}
