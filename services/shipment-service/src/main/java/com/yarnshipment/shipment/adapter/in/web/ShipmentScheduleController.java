package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentScheduleApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.UpdateShipmentDateRequest;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateCommand;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Taslak sevkiyatın gününü değiştirme ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentScheduleController implements ShipmentScheduleApi {

    private final UpdateShipmentDateUseCase updateShipmentDateUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentScheduleController(
            UpdateShipmentDateUseCase updateShipmentDateUseCase, CurrentActorProvider currentActorProvider) {
        this.updateShipmentDateUseCase = updateShipmentDateUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public ResponseEntity<Void> updateShipmentDate(UUID shipmentId, UpdateShipmentDateRequest request) {
        updateShipmentDateUseCase.updateShipmentDate(new UpdateShipmentDateCommand(
                currentActorProvider.getCurrentActor(), shipmentId, request.getShipmentDate()));
        return ResponseEntity.noContent().build();
    }
}
