package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import java.util.UUID;

/** Lokasyon ve ambalaj tipi için standart daranın tanımlanması; varsa değerleri günceller. */
public interface SaveTareStandardUseCase {

    TareStandard saveTareStandard(Actor actor, UUID siteId, PackagingType packagingType, TareStandardDetails details);
}
