package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentCorrectionApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.CorrectionListResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.CorrectionView;
import com.yarnshipment.shipment.adapter.in.web.api.model.RecordCorrectionRequest;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.correction.GetCorrectionsUseCase;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionCommand;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Gerekçeli düzeltme uçları (rapor 7.8). Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ShipmentCorrectionController implements ShipmentCorrectionApi {

    private final RecordCorrectionUseCase recordCorrectionUseCase;
    private final GetCorrectionsUseCase getCorrectionsUseCase;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentCorrectionController(
            RecordCorrectionUseCase recordCorrectionUseCase,
            GetCorrectionsUseCase getCorrectionsUseCase,
            CurrentActorProvider currentActorProvider) {
        this.recordCorrectionUseCase = recordCorrectionUseCase;
        this.getCorrectionsUseCase = getCorrectionsUseCase;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('CORRECTION_CREATE')")
    public ResponseEntity<CorrectionView> recordCorrection(
            UUID shipmentId, UUID idempotencyKey, RecordCorrectionRequest recordCorrectionRequest) {
        RecordCorrectionCommand command = new RecordCorrectionCommand(
                currentActorProvider.getCurrentActor(),
                shipmentId,
                idempotencyKey,
                CorrectionViews.toRequest(recordCorrectionRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CorrectionViews.toView(recordCorrectionUseCase.recordCorrection(command)));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('CORRECTION_CREATE', 'AUDIT_VIEW')")
    public ResponseEntity<CorrectionListResponse> listCorrections(UUID shipmentId) {
        GetShipmentHistoryQuery query = new GetShipmentHistoryQuery(currentActorProvider.getCurrentActor(), shipmentId);
        return ResponseEntity.ok(new CorrectionListResponse(getCorrectionsUseCase.getCorrections(query).stream()
                .map(CorrectionViews::toView)
                .toList()));
    }
}
