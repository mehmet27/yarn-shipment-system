package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadCommand;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceInvalidationPort;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceStatus;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yüklemeyi onaylar. Hash'i istemci söylemez; sunucu dosyayı nesne deposundan okuyup hesaplar (SEC-09). Yalnızca
 * isteyen onaylayabilir; tekrar onay (zayıf bağlantıda yeniden deneme) aynı kaydı döner. Yeni bir kantar fişi
 * onaylanınca aynı sevkiyatın önceki geçerli kantar fişleri kendiliğinden geçersiz kılınır.
 */
@Service
public class ConfirmUploadService implements ConfirmUploadUseCase {

    private static final String ACTION = "EVIDENCE_UPLOAD_CONFIRM";
    private static final String SUPERSEDED_REASON = "Yeni kantar fişi yüklendi";

    private final UploadIntentPort intents;
    private final ObjectStoragePort storage;
    private final EvidenceStorePort evidence;
    private final EvidenceInvalidationPort invalidations;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ConfirmUploadService(
            UploadIntentPort intents,
            ObjectStoragePort storage,
            EvidenceStorePort evidence,
            EvidenceInvalidationPort invalidations,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.intents = intents;
        this.storage = storage;
        this.evidence = evidence;
        this.invalidations = invalidations;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EvidenceFile confirmUpload(ConfirmUploadCommand command) {
        UploadIntent intent = intents.findIntent(command.evidenceId())
                .filter(found -> found.requestedBy().equals(command.actor().userId()))
                .orElseThrow(() -> new UploadIntentNotFoundException(command.evidenceId()));
        scopeGuard.validateInScope(command.actor(), ShipmentScopes.of(intent.shipmentId(), intent.siteId()), ACTION);
        Optional<EvidenceFile> existing = evidence.findEvidence(intent.id());
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        StoredObject stored =
                storage.inspect(intent.storageKey()).orElseThrow(() -> new UploadNotCompletedException(intent.id()));
        EvidenceFile file = EvidenceFile.confirm(intent, stored, clock.instant());
        evidence.saveEvidence(file);
        if (file.declared().type() == EvidenceType.DISPATCH_SCALE) {
            supersedePreviousTickets(file, command.actor().userId());
        }
        return file;
    }

    /** Yeni kantar fişi yüklenince aynı sevkiyatın önceki geçerli kantar fişleri geçersiz olur; dosyalar silinmez. */
    private void supersedePreviousTickets(EvidenceFile current, UUID actorId) {
        Instant now = clock.instant();
        evidence.findByShipment(current.shipmentId()).stream()
                .filter(previous -> !previous.id().equals(current.id()))
                .filter(previous -> previous.status() == EvidenceStatus.VALID)
                .filter(previous -> previous.declared().type() == EvidenceType.DISPATCH_SCALE)
                .map(previous -> previous.invalidate(SUPERSEDED_REASON))
                .forEach(invalidated -> invalidations.saveInvalidation(invalidated, actorId, now));
    }
}
