package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentWeightDecisionApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.WeightDecisionLine;
import com.yarnshipment.shipment.adapter.in.web.api.model.WeightDecisionRequest;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchCommand;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchUseCase;
import com.yarnshipment.shipment.domain.model.Recipe;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kantar uyuşmazlığı karar ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentWeightDecisionController implements ShipmentWeightDecisionApi {

    private final ResolveWeightMismatchUseCase resolveUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentWeightDecisionController(
            ResolveWeightMismatchUseCase resolveUseCase, CurrentActorProvider currentActorProvider) {
        this.resolveUseCase = resolveUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    private static List<Recipe.LineWeight> lines(WeightDecisionRequest request) {
        List<WeightDecisionLine> lines = request.getLines();
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(line -> new Recipe.LineWeight(line.getLineNo(), new BigDecimal(line.getNetKg())))
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_CANCEL')")
    public ResponseEntity<Void> decideWeightMismatch(UUID shipmentId, WeightDecisionRequest request) {
        resolveUseCase.resolve(new ResolveWeightMismatchCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                WeightDecision.valueOf(request.getDecision().name()),
                request.getReason(),
                lines(request)));
        return ResponseEntity.noContent().build();
    }
}
