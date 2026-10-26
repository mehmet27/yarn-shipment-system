package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentArrivalApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ArrivalDeclarationRequest;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalCommand;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Şoför varış beyanı ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentArrivalController implements ShipmentArrivalApi {

    private final DeclareArrivalUseCase declareArrivalUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentArrivalController(
            DeclareArrivalUseCase declareArrivalUseCase, CurrentActorProvider currentActorProvider) {
        this.declareArrivalUseCase = declareArrivalUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DRIVER_DECLARATION_RECORD')")
    public ResponseEntity<Void> declareArrival(
            UUID shipmentId, UUID idempotencyKey, ArrivalDeclarationRequest arrivalDeclarationRequest) {
        declareArrivalUseCase.declareArrival(new DeclareArrivalCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                idempotencyKey,
                arrivalDeclarationRequest.getSealIntact()));
        return ResponseEntity.noContent().build();
    }
}
