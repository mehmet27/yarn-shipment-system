package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.QualityClaimsApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.InspectionView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.OpenQualityClaimRequest;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.QualityClaimView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.RecordInspectionResultRequest;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.RequestInspectionRequest;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ShipmentClaimsView;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsQuery;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionUseCase;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kalite itirazı ve numune/lab uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class QualityClaimController implements QualityClaimsApi {

    private final GetShipmentClaimsUseCase getShipmentClaims;
    private final OpenQualityClaimUseCase openQualityClaim;
    private final RequestInspectionUseCase requestInspection;
    private final RecordInspectionResultUseCase recordInspectionResult;
    private final CurrentActorProvider currentActorProvider;

    public QualityClaimController(
            GetShipmentClaimsUseCase getShipmentClaims,
            OpenQualityClaimUseCase openQualityClaim,
            RequestInspectionUseCase requestInspection,
            RecordInspectionResultUseCase recordInspectionResult,
            CurrentActorProvider currentActorProvider) {
        this.getShipmentClaims = getShipmentClaims;
        this.openQualityClaim = openQualityClaim;
        this.requestInspection = requestInspection;
        this.recordInspectionResult = recordInspectionResult;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_VIEW')")
    public ResponseEntity<ShipmentClaimsView> getShipmentClaims(UUID shipmentId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(QualityClaimViews.toView(
                getShipmentClaims.getShipmentClaims(new GetShipmentClaimsQuery(actor, shipmentId)), actor));
    }

    @Override
    @PreAuthorize("hasAuthority('QUALITY_CLAIM_OPEN')")
    public ResponseEntity<QualityClaimView> openQualityClaim(OpenQualityClaimRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        OpenQualityClaimCommand command = new OpenQualityClaimCommand(
                actor,
                request.getShipmentId(),
                new ClaimRequest(request.getDescription(), request.getBagKeys(), request.getEvidenceIds()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QualityClaimViews.toView(openQualityClaim.openQualityClaim(command), actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_RESOLVE')")
    public ResponseEntity<InspectionView> requestInspection(UUID claimId, RequestInspectionRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        RequestInspectionCommand command = new RequestInspectionCommand(
                actor, claimId, InspectionKind.valueOf(request.getKind().getValue()), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QualityClaimViews.toView(requestInspection.requestInspection(command), actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_RESOLVE')")
    public ResponseEntity<InspectionView> recordInspectionResult(
            UUID claimId, UUID inspectionId, RecordInspectionResultRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        RecordInspectionResultCommand command = new RecordInspectionResultCommand(
                actor, claimId, inspectionId, request.getResult(), request.getEvidenceId());
        return ResponseEntity.ok(
                QualityClaimViews.toView(recordInspectionResult.recordInspectionResult(command), actor));
    }
}
