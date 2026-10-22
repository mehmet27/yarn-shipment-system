package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentBagsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.AssignShipmentBagsRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.AssignShipmentBagsResponse;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Çuval atama ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentBagAssignmentController implements ShipmentBagsApi {

    private final AssignBagsUseCase assignBagsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentBagAssignmentController(
            AssignBagsUseCase assignBagsUseCase, CurrentActorProvider currentActorProvider) {
        this.assignBagsUseCase = assignBagsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISPATCH_RECORD')")
    public ResponseEntity<AssignShipmentBagsResponse> assignShipmentBags(
            UUID shipmentId, AssignShipmentBagsRequest request) {
        AssignBagsCommand command = new AssignBagsCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                List.copyOf(request.getUniqueKeys()),
                ScanMethod.valueOf(request.getScanMethod().name()));
        return ResponseEntity.ok(new AssignShipmentBagsResponse(assignBagsUseCase.assignBags(command)));
    }
}
