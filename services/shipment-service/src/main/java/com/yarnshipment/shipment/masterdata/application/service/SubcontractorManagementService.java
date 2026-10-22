package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSubcontractorStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fasoncu ekler, günceller, pasife alır. Yetki uçta verilir; burada yalnızca kuralları uygular. */
@Service
public class SubcontractorManagementService
        implements CreateSubcontractorUseCase, UpdateSubcontractorUseCase, ChangeSubcontractorStatusUseCase {

    private static final String KIND = "Subcontractor";

    private final MasterDataCommandPort commands;

    public SubcontractorManagementService(MasterDataCommandPort commands) {
        this.commands = commands;
    }

    @Override
    @Transactional
    public Subcontractor createSubcontractor(Actor actor, String title) {
        return commands.insertSubcontractor(actor.userId(), MasterDataText.clean("title", title));
    }

    @Override
    @Transactional
    public Subcontractor updateSubcontractor(Actor actor, UUID id, String title) {
        String cleaned = MasterDataText.clean("title", title);
        if (!commands.updateSubcontractor(actor.userId(), id, cleaned)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
        return new Subcontractor(id, cleaned);
    }

    @Override
    @Transactional
    public void changeSubcontractorStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!commands.setSubcontractorStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
