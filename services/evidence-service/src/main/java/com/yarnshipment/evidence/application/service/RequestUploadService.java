package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.RequestUploadCommand;
import com.yarnshipment.evidence.application.port.in.RequestUploadUseCase;
import com.yarnshipment.evidence.application.port.in.UploadTicket;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedUpload;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.UploadTarget;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.ShipmentLockedException;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yükleme isteğini kabul eder: izin, dosya sınırları, sevkiyatın görünürlüğü ve kilidi denetlenir; sonra imzalı yükleme
 * adresi verilir. Kanıt kaydı henüz yazılmaz; onayda yazılır.
 */
@Service
public class RequestUploadService implements RequestUploadUseCase {

    private static final String ACTION = "EVIDENCE_UPLOAD";

    private final ShipmentAccessPort shipments;
    private final UploadIntentPort intents;
    private final ObjectStoragePort storage;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public RequestUploadService(
            ShipmentAccessPort shipments,
            UploadIntentPort intents,
            ObjectStoragePort storage,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.shipments = shipments;
        this.intents = intents;
        this.storage = storage;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UploadTicket requestUpload(RequestUploadCommand command) {
        Actor actor = command.actor();
        UploadIntent.DeclaredFile file = command.file();
        UploadPermissions.requireAllowed(actor, file);
        ShipmentSnapshot shipment = shipments.findAccessible(command.shipmentId());
        scopeGuard.validateInScope(actor, ShipmentScopes.of(actor, shipment.shipmentId(), shipment.siteId()), ACTION);
        if (shipment.locked()) {
            throw new ShipmentLockedException(shipment.shipmentId());
        }
        Instant now = clock.instant();
        UUID evidenceId = UUID.randomUUID();
        String storageKey = UploadIntent.storageKeyOf(now, shipment.shipmentId(), evidenceId);
        PresignedUpload upload = storage.presignUpload(new UploadTarget(storageKey, file.mimeType(), file.sizeBytes()));
        intents.saveIntent(new UploadIntent(
                evidenceId,
                shipment.shipmentId(),
                shipment.siteId(),
                file,
                storageKey,
                actor.userId(),
                now,
                upload.expiresAt()));
        return new UploadTicket(evidenceId, upload.url(), upload.headers(), upload.expiresAt());
    }
}
