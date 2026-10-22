package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeVehicleStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.VehicleSiteCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.PlateNumber;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Araç ekler, günceller, pasife alır. Plaka normalleştirilir; yetki uçta verilir. */
@Service
public class VehicleManagementService
        implements CreateVehicleUseCase, UpdateVehicleUseCase, ChangeVehicleStatusUseCase {

    private static final String KIND = "Vehicle";

    private final VehicleSiteCommandPort commands;

    public VehicleManagementService(VehicleSiteCommandPort commands) {
        this.commands = commands;
    }

    @Override
    @Transactional
    public Vehicle createVehicle(Actor actor, String plateNo) {
        return commands.insertVehicle(actor.userId(), PlateNumber.normalize(plateNo));
    }

    @Override
    @Transactional
    public Vehicle updateVehicle(Actor actor, UUID id, String plateNo) {
        String plate = PlateNumber.normalize(plateNo);
        if (!commands.updateVehicle(actor.userId(), id, plate)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
        return new Vehicle(id, plate);
    }

    @Override
    @Transactional
    public void changeVehicleStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!commands.setVehicleStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
