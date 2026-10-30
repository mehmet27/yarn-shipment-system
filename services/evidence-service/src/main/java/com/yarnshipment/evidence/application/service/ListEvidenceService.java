package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.ListEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.ListEvidenceUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sevkiyatın kanıtlarını listeler. Sevkiyat isteyene görünmüyorsa istek reddedilir; görünüyorsa yalnızca kör giriş
 * kuralının izin verdiği türler döner, geri kalanı listede hiç yer almaz.
 */
@Service
public class ListEvidenceService implements ListEvidenceUseCase {

    private static final String ACTION = "EVIDENCE_VIEW";

    private final EvidenceStorePort evidence;
    private final ShipmentAccessPort shipments;
    private final ScopeGuard scopeGuard;

    public ListEvidenceService(EvidenceStorePort evidence, ShipmentAccessPort shipments, ScopeGuard scopeGuard) {
        this.evidence = evidence;
        this.shipments = shipments;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceFile> listEvidence(ListEvidenceQuery query) {
        Actor actor = query.actor();
        ShipmentSnapshot shipment = shipments.findAccessible(query.shipmentId());
        scopeGuard.validateInScope(actor, ShipmentScopes.of(actor, shipment.shipmentId(), shipment.siteId()), ACTION);
        return evidence.findByShipment(shipment.shipmentId()).stream()
                .filter(file ->
                        EvidenceVisibility.mayView(actor, file.declared().type(), shipment.comparisonDone()))
                .toList();
    }
}
