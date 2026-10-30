package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsQuery;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyatın itiraz dosyasını okur. */
@Service
public class GetShipmentClaimsService implements GetShipmentClaimsUseCase {

    private static final String ACTION = "QUALITY_CLAIM_VIEW";

    private final QualityClaimPort claims;
    private final ClaimAccess access;
    private final Clock clock;

    public GetShipmentClaimsService(
            QualityClaimPort claims,
            DiscrepancyQueryPort discrepancies,
            InspectionPort inspections,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.claims = claims;
        this.access = new ClaimAccess(claims, discrepancies, inspections, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentClaims getShipmentClaims(GetShipmentClaimsQuery query) {
        ClaimTarget target = access.loadTarget(query.actor(), query.shipmentId(), ACTION);
        return new ShipmentClaims(
                target,
                target.isOpenAt(clock.instant()),
                claims.findClaims(target.shipmentId()).stream()
                        .map(access::fileOf)
                        .toList());
    }
}
