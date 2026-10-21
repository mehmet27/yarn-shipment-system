package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentCreationApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.CreateShipmentRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.CreateShipmentResponse;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreateShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Sevkiyat taslağı oluşturma ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentCreationController implements ShipmentCreationApi {

    private final CreateShipmentUseCase createShipmentUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentCreationController(
            CreateShipmentUseCase createShipmentUseCase, CurrentActorProvider currentActorProvider) {
        this.createShipmentUseCase = createShipmentUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public ResponseEntity<CreateShipmentResponse> createShipment(
            UUID idempotencyKey, CreateShipmentRequest createShipmentRequest) {
        CreateShipmentCommand command = ShipmentCreations.toCommand(
                currentActorProvider.getCurrentActor(), idempotencyKey, createShipmentRequest);
        CreatedShipment created = createShipmentUseCase.createShipment(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateShipmentResponse(created.id(), created.shipmentNo()));
    }
}
