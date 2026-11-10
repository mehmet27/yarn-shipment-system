package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.TrackShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentUseCase;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** İzlenen sevkiyatın adım zamanlarını yazar; izlenmeyen sevkiyatın adımı yok sayılır. */
@Service
public class TrackShipmentService implements TrackShipmentUseCase {

    private final ShipmentWatchPort watchPort;

    public TrackShipmentService(ShipmentWatchPort watchPort) {
        this.watchPort = watchPort;
    }

    @Override
    @Transactional
    public void trackShipment(TrackShipmentCommand command) {
        switch (command.milestone()) {
            case DISPATCHED -> watchPort.recordDispatched(command.shipmentId(), command.at());
            case CLOSED -> watchPort.recordClosed(command.shipmentId(), command.at());
        }
    }
}
