package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadCommand;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yüklemeyi onaylar. Hash'i istemci söylemez; sunucu dosyayı nesne deposundan okuyup hesaplar (SEC-09). Yalnızca
 * isteyen onaylayabilir; tekrar onay (zayıf bağlantıda yeniden deneme) aynı kaydı döner.
 */
@Service
public class ConfirmUploadService implements ConfirmUploadUseCase {

    private static final String ACTION = "EVIDENCE_UPLOAD_CONFIRM";

    private final UploadIntentPort intents;
    private final ObjectStoragePort storage;
    private final EvidenceStorePort evidence;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ConfirmUploadService(
            UploadIntentPort intents,
            ObjectStoragePort storage,
            EvidenceStorePort evidence,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.intents = intents;
        this.storage = storage;
        this.evidence = evidence;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvidenceFile confirmUpload(ConfirmUploadCommand command) {
        UploadIntent intent = intents.findIntent(command.evidenceId())
                .filter(found -> found.requestedBy().equals(command.actor().userId()))
                .orElseThrow(() -> new UploadIntentNotFoundException(command.evidenceId()));
        scopeGuard.validateInScope(
                command.actor(), ShipmentScopes.of(command.actor(), intent.shipmentId(), intent.siteId()), ACTION);
        Optional<EvidenceFile> existing = evidence.findEvidence(intent.id());
        if (existing.isPresent()) {
            return existing.get();
        }
        StoredObject stored =
                storage.inspect(intent.storageKey()).orElseThrow(() -> new UploadNotCompletedException(intent.id()));
        EvidenceFile file = EvidenceFile.confirm(intent, stored, clock.instant());
        evidence.saveEvidence(file);
        return file;
    }
}
