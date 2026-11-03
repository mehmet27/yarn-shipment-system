package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.BusinessDays;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kalite itirazını açar: süre, çuval ve fotoğraf kuralları domain'de, kanıtın sevkiyata aitliği burada denetlenir. */
@Service
public class OpenQualityClaimService implements OpenQualityClaimUseCase {

    private static final String ACTION = "QUALITY_CLAIM_OPEN";
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    private final QualityClaimPort claims;
    private final ClaimAccess access;
    private final ClaimEvidence evidence;
    private final Clock clock;

    public OpenQualityClaimService(
            QualityClaimPort claims,
            DiscrepancyQueryPort discrepancies,
            InspectionPort inspections,
            EvidenceLookupPort evidence,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.claims = claims;
        this.access = new ClaimAccess(claims, discrepancies, inspections, scopeGuard);
        this.evidence = new ClaimEvidence(evidence);
        this.clock = clock;
    }

    @Override
    @Transactional
    public ClaimFile openQualityClaim(OpenQualityClaimCommand command) {
        ClaimTarget target = access.loadTarget(command.actor(), command.shipmentId(), ACTION);
        ClaimRequest request = command.request();
        Instant now = clock.instant();
        target.requireClaimable(request, claimedKeys(target), now);
        evidence.requireAll(target.shipmentId(), request.evidenceIds(), ClaimEvidence.QUALITY_CLAIM);
        QualityClaim claim = claims.saveClaim(new NewQualityClaim(
                target,
                request,
                command.actor().userId(),
                now,
                BusinessDays.plus(now, BusinessDays.DISCREPANCY_DUE_DAYS, FACTORY_ZONE)));
        return access.fileOf(claim);
    }

    private List<String> claimedKeys(ClaimTarget target) {
        return claims.findClaims(target.shipmentId()).stream()
                .flatMap(claim -> claim.bagKeys().stream())
                .toList();
    }
}
