package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kanıt üst verisini okur. Kapsam sevkiyat üzerinden belirlenir; sevkiyat ya da kanıt türü (kör giriş) isteyene
 * görünmüyorsa kanıtın varlığı da açığa çıkmaz (404). Dosyanın kendisi bu uçtan verilmez.
 */
@Service
public class GetEvidenceService implements GetEvidenceUseCase {

    private static final String ACTION = "EVIDENCE_VIEW";

    private final EvidenceStorePort evidence;
    private final ShipmentAccessPort shipments;
    private final ScopeGuard scopeGuard;

    public GetEvidenceService(EvidenceStorePort evidence, ShipmentAccessPort shipments, ScopeGuard scopeGuard) {
        this.evidence = evidence;
        this.shipments = shipments;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceFile getEvidence(GetEvidenceQuery query) {
        Actor actor = query.actor();
        EvidenceFile file = evidence.findEvidence(query.evidenceId())
                .orElseThrow(() -> new EvidenceNotFoundException(query.evidenceId()));
        ShipmentSnapshot shipment;
        try {
            shipment = shipments.findAccessible(file.shipmentId());
        } catch (ShipmentNotAccessibleException exception) {
            throw new EvidenceNotFoundException(query.evidenceId());
        }
        scopeGuard.validateInScope(actor, ShipmentScopes.of(actor, shipment.shipmentId(), shipment.siteId()), ACTION);
        if (!EvidenceVisibility.mayView(actor, file.declared().type(), shipment.comparisonDone())) {
            throw new EvidenceNotFoundException(query.evidenceId());
        }
        return file;
    }
}
