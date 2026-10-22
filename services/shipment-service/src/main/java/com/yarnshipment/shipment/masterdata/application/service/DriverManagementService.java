package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeDriverStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Şoför ekler, günceller, pasife alır. Bağlı fasoncu verilirse etkin olmalıdır. */
@Service
public class DriverManagementService implements CreateDriverUseCase, UpdateDriverUseCase, ChangeDriverStatusUseCase {

    private static final String KIND = "Driver";

    private final MasterDataCommandPort commands;

    public DriverManagementService(MasterDataCommandPort commands) {
        this.commands = commands;
    }

    @Override
    @Transactional
    public Driver createDriver(Actor actor, DriverDetails details) {
        requireActiveSubcontractor(details);
        return commands.insertDriver(actor.userId(), details);
    }

    @Override
    @Transactional
    public Driver updateDriver(Actor actor, UUID id, DriverDetails details) {
        requireActiveSubcontractor(details);
        if (!commands.updateDriver(actor.userId(), id, details)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
        return new Driver(id, details.fullName(), details.subcontractorId());
    }

    @Override
    @Transactional
    public void changeDriverStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!commands.setDriverStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }

    private void requireActiveSubcontractor(DriverDetails details) {
        UUID subcontractorId = details.subcontractorId();
        if (subcontractorId != null && !commands.activeSubcontractorExists(subcontractorId)) {
            throw new InvalidMasterDataException("subcontractorId", "unknown or inactive");
        }
    }
}
