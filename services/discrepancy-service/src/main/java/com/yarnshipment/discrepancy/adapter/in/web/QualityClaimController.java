package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.QualityClaimsApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.OpenQualityClaimRequest;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.QualityClaimView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ShipmentClaimsView;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsQuery;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimUseCase;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kalite itirazı uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class QualityClaimController implements QualityClaimsApi {

    private final GetShipmentClaimsUseCase getShipmentClaims;
    private final OpenQualityClaimUseCase openQualityClaim;
    private final CurrentActorProvider currentActorProvider;

    public QualityClaimController(
            GetShipmentClaimsUseCase getShipmentClaims,
            OpenQualityClaimUseCase openQualityClaim,
            CurrentActorProvider currentActorProvider) {
        this.getShipmentClaims = getShipmentClaims;
        this.openQualityClaim = openQualityClaim;
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
                new ClaimRequest(
                        request.getDescription(),
                        new BigDecimal(request.getClaimedKg()),
                        request.getFabricReceiptId(),
                        request.getEvidenceIds()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QualityClaimViews.toView(openQualityClaim.openQualityClaim(command), actor));
    }
}
