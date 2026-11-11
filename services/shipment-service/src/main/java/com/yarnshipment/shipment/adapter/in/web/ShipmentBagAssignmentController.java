package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentBagsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.AssignShipmentBagsRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.AssignShipmentBagsResponse;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase.RemoveBagCommand;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Çuval atama ve çıkarma uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentBagAssignmentController implements ShipmentBagsApi {

    private final AssignBagsUseCase assignBagsUseCase;
    private final RemoveBagUseCase removeBagUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentBagAssignmentController(
            AssignBagsUseCase assignBagsUseCase,
            RemoveBagUseCase removeBagUseCase,
            CurrentActorProvider currentActorProvider) {
        this.assignBagsUseCase = assignBagsUseCase;
        this.removeBagUseCase = removeBagUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<AssignShipmentBagsResponse> assignShipmentBags(
            UUID shipmentId, AssignShipmentBagsRequest request) {
        AssignBagsCommand command = new AssignBagsCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                request.getWeightsKg().stream().map(BigDecimal::new).toList());
        return ResponseEntity.ok(new AssignShipmentBagsResponse(assignBagsUseCase.assignBags(command)));
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<Void> removeShipmentBag(UUID shipmentId, Integer seqNo) {
        removeBagUseCase.removeBag(new RemoveBagCommand(currentActorProvider.getCurrentActor(), shipmentId, seqNo));
        return ResponseEntity.noContent().build();
    }
}
