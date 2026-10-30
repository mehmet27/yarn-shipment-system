package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.claim.QualityClaimNotFoundException;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotFoundException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import java.util.UUID;

/**
 * İtirazları yükler ve kapsam kontrolünden geçirir. Kapsam sevkiyat üzerindendir: fasoncu yalnızca kendi
 * sevkiyatına itiraz açar ve onun itirazlarını görür, iç kullanıcılar yetkili oldukları lokasyonları görür.
 */
final class ClaimAccess {

    private static final String RESOURCE_TYPE = "Shipment";

    private final QualityClaimPort claims;
    private final DiscrepancyQueryPort discrepancies;
    private final InspectionPort inspections;
    private final ScopeGuard scopeGuard;

    ClaimAccess(
            QualityClaimPort claims,
            DiscrepancyQueryPort discrepancies,
            InspectionPort inspections,
            ScopeGuard scopeGuard) {
        this.claims = claims;
        this.discrepancies = discrepancies;
        this.inspections = inspections;
        this.scopeGuard = scopeGuard;
    }

    /** Karşılaştırılmamış ya da lokasyonu bilinmeyen teslime itiraz açılamaz. */
    ClaimTarget loadTarget(Actor actor, UUID shipmentId, String action) {
        ClaimTarget target = claims.findTarget(shipmentId)
                .orElseThrow(
                        () -> new ClaimRejectedException(ClaimRejection.NOT_AVAILABLE, "shipmentId=" + shipmentId));
        scopeGuard.validateInScope(
                actor,
                new ScopedResource(RESOURCE_TYPE, shipmentId, target.siteId(), target.subcontractorId(), null),
                action);
        return target;
    }

    QualityClaim loadClaim(Actor actor, UUID claimId, String action) {
        QualityClaim claim = claims.findClaim(claimId).orElseThrow(() -> new QualityClaimNotFoundException(claimId));
        loadTarget(actor, claim.shipmentId(), action);
        return claim;
    }

    /** İtirazın uyuşmazlığı; karara bağlanmışsa kontrol istenemez ve sonuç girilemez. */
    Discrepancy unresolvedDiscrepancyOf(QualityClaim claim) {
        Discrepancy discrepancy = discrepancyOf(claim);
        if (!discrepancy.status().isUnresolved()) {
            throw new DiscrepancyNotOpenException(discrepancy.id(), discrepancy.status());
        }
        return discrepancy;
    }

    ClaimFile fileOf(QualityClaim claim) {
        return new ClaimFile(claim, discrepancyOf(claim), inspections.findInspections(claim.discrepancyId()));
    }

    private Discrepancy discrepancyOf(QualityClaim claim) {
        return discrepancies
                .findDiscrepancy(claim.discrepancyId())
                .orElseThrow(() -> new DiscrepancyNotFoundException(claim.discrepancyId()));
    }
}
