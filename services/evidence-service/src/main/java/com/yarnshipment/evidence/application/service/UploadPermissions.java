package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent.DeclaredFile;
import com.yarnshipment.evidence.domain.model.UploadNotAllowedException;
import com.yarnshipment.evidence.domain.model.UploadRules;
import com.yarnshipment.evidence.domain.model.UploadStage;
import java.util.Map;

/** Kanıt türünün aşamasına göre gereken yükleme izni (yetkilendirme analizi, EVIDENCE_UPLOAD_*). */
final class UploadPermissions {

    private static final Map<UploadStage, Permission> BY_STAGE = Map.of(
            UploadStage.DISPATCH, Permission.EVIDENCE_UPLOAD_DISPATCH,
            UploadStage.DRIVER, Permission.EVIDENCE_UPLOAD_DRIVER,
            UploadStage.DELIVERY, Permission.EVIDENCE_UPLOAD_DELIVERY,
            UploadStage.CLAIM, Permission.EVIDENCE_UPLOAD_CLAIM);

    private UploadPermissions() {}

    static boolean mayUpload(Actor actor, EvidenceType type) {
        return type.stages().stream().map(BY_STAGE::get).anyMatch(actor::hasPermission);
    }

    /** İzin, dosya türü ve boyut sınırlarından biri tutmuyorsa {@link UploadNotAllowedException} fırlatır. */
    static void requireAllowed(Actor actor, DeclaredFile file) {
        if (!mayUpload(actor, file.type())) {
            throw new UploadNotAllowedException("type=" + file.type());
        }
        if (!UploadRules.isAllowedMimeType(file.mimeType()) || !UploadRules.isAllowedSize(file.sizeBytes())) {
            throw new UploadNotAllowedException("file");
        }
    }
}
