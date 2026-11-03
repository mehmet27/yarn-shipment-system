package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListTareStandardsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.SaveTareStandardUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.TareStandardPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Standart dara listesi ve tanımlama. Yazma yetkisi uçta verilir. */
@Service
public class TareStandardManagementService implements ListTareStandardsUseCase, SaveTareStandardUseCase {

    private final TareStandardPort standards;

    public TareStandardManagementService(TareStandardPort standards) {
        this.standards = standards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TareStandard> listTareStandards(@Nullable UUID siteId) {
        return standards.findTareStandards(siteId);
    }

    @Override
    @Transactional
    public TareStandard saveTareStandard(
            Actor actor, UUID siteId, PackagingType packagingType, TareStandardDetails details) {
        return standards.upsertTareStandard(actor.userId(), siteId, packagingType, details);
    }
}
