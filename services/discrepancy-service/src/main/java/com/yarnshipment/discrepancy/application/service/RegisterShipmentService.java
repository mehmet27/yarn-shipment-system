package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentUseCase;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Oluşturulan sevkiyatı izlemeye alır; olay tekrar gelirse ilk kayıt kalır. */
@Service
public class RegisterShipmentService implements RegisterShipmentUseCase {

    private final ShipmentWatchPort watchPort;

    public RegisterShipmentService(ShipmentWatchPort watchPort) {
        this.watchPort = watchPort;
    }

    @Override
    @Transactional
    public void registerShipment(RegisterShipmentCommand command) {
        watchPort.recordCreated(
                new WatchedShipment(
                        command.shipmentId(), command.shipmentNo(), command.siteId(), command.subcontractorId(), null),
                command.createdAt());
    }
}
