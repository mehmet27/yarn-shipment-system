package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceCommand;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceInvalidationPort;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kanıtı geçersiz kılar (yalnızca {@code EVIDENCE_INVALIDATE}, yani admin). Gerekçe zorunludur; kanıtı görmeyen biri
 * için kanıt yoktur (404). Dosya ve kayıt silinmez, olay yayınlanır.
 */
@Service
public class InvalidateEvidenceService implements InvalidateEvidenceUseCase {

    private static final String ACTION = "EVIDENCE_INVALIDATE";

    private final EvidenceStorePort evidence;
    private final EvidenceInvalidationPort invalidations;
    private final ShipmentAccessPort shipments;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public InvalidateEvidenceService(
            EvidenceStorePort evidence,
            EvidenceInvalidationPort invalidations,
            ShipmentAccessPort shipments,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.evidence = evidence;
        this.invalidations = invalidations;
        this.shipments = shipments;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvidenceFile invalidateEvidence(InvalidateEvidenceCommand command) {
        Actor actor = command.actor();
        if (!actor.hasPermission(Permission.EVIDENCE_INVALIDATE)) {
            throw new EvidenceNotFoundException(command.evidenceId());
        }
        EvidenceFile file = evidence.findEvidence(command.evidenceId())
                .orElseThrow(() -> new EvidenceNotFoundException(command.evidenceId()));
        ShipmentSnapshot shipment;
        try {
            shipment = shipments.findAccessible(file.shipmentId());
        } catch (ShipmentNotAccessibleException exception) {
            throw new EvidenceNotFoundException(command.evidenceId());
        }
        scopeGuard.validateInScope(actor, ShipmentScopes.of(actor, shipment.shipmentId(), shipment.siteId()), ACTION);
        EvidenceFile invalidated = file.invalidate(command.reason());
        Instant now = clock.instant();
        invalidations.saveInvalidation(invalidated, actor.userId(), now);
        return invalidated;
    }
}
