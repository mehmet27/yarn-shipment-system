package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentCancellationApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.CancelShipmentRequest;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CancelShipmentUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Sevkiyat iptal ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentCancellationController implements ShipmentCancellationApi {

    private final CancelShipmentUseCase cancelShipmentUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentCancellationController(
            CancelShipmentUseCase cancelShipmentUseCase, CurrentActorProvider currentActorProvider) {
        this.cancelShipmentUseCase = cancelShipmentUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CANCEL')")
    public ResponseEntity<Void> cancelShipment(
            UUID shipmentId, UUID idempotencyKey, CancelShipmentRequest cancelShipmentRequest) {
        cancelShipmentUseCase.cancelShipment(new CancelShipmentCommand(
                currentActorProvider.getCurrentActor(), shipmentId, cancelShipmentRequest.getReason(), idempotencyKey));
        return ResponseEntity.noContent().build();
    }
}
