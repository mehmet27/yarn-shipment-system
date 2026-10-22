package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSiteStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSiteUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSiteUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.VehicleSiteCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lokasyon ekler, günceller, pasife alır. Yetki uçta verilir; burada yalnızca kuralları uygular. */
@Service
public class SiteManagementService implements CreateSiteUseCase, UpdateSiteUseCase, ChangeSiteStatusUseCase {

    private static final String KIND = "Site";

    private final VehicleSiteCommandPort commands;

    public SiteManagementService(VehicleSiteCommandPort commands) {
        this.commands = commands;
    }

    @Override
    @Transactional
    public Site createSite(Actor actor, String name) {
        return commands.insertSite(actor.userId(), MasterDataText.clean("name", name));
    }

    @Override
    @Transactional
    public Site updateSite(Actor actor, UUID id, String name) {
        String cleaned = MasterDataText.clean("name", name);
        if (!commands.updateSite(actor.userId(), id, cleaned)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
        return new Site(id, cleaned);
    }

    @Override
    @Transactional
    public void changeSiteStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!commands.setSiteStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
