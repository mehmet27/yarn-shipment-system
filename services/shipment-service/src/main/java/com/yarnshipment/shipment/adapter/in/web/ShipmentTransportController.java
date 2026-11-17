package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentTransportApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.UpdateShipmentTransportRequest;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportCommand;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportUseCase;
import com.yarnshipment.shipment.domain.transport.TransportAssignment;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Şoför ve araç düzeltme ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentTransportController implements ShipmentTransportApi {

    private final UpdateShipmentTransportUseCase updateTransportUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentTransportController(
            UpdateShipmentTransportUseCase updateTransportUseCase, CurrentActorProvider currentActorProvider) {
        this.updateTransportUseCase = updateTransportUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public ResponseEntity<Void> updateShipmentTransport(UUID shipmentId, UpdateShipmentTransportRequest request) {
        updateTransportUseCase.updateShipmentTransport(new UpdateShipmentTransportCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                new TransportAssignment(
                        request.getDriverId(), request.getDriverName(), request.getVehicleId(), request.getPlateNo())));
        return ResponseEntity.noContent().build();
    }
}
