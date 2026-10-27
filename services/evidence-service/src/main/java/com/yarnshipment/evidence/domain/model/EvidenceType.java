package com.yarnshipment.evidence.domain.model;

import java.util.List;

/** Kanıt türleri (veritabanındaki {@code evidence_type} kısıtıyla aynı) ve yüklenebildikleri aşamalar. */
public enum EvidenceType {
    DISPATCH_SCALE,
    BAG_OVERVIEW,
    BAG_LABEL,
    VEHICLE_PLATE,
    SEAL,
    DELIVERY_SCALE,
    DELIVERY_BAGS,
    QUALITY_CLAIM,
    SIGNED_DELIVERY_DOC,
    LAB_RESULT;

    /** Bu tür hangi aşamalardaki izinlerle yüklenebilir. */
    public List<UploadStage> stages() {
        return switch (this) {
            case DISPATCH_SCALE, BAG_OVERVIEW, BAG_LABEL -> List.of(UploadStage.DISPATCH);
            case VEHICLE_PLATE -> List.of(UploadStage.DRIVER);
            case SEAL -> List.of(UploadStage.DISPATCH, UploadStage.DELIVERY);
            case DELIVERY_SCALE, DELIVERY_BAGS, SIGNED_DELIVERY_DOC -> List.of(UploadStage.DELIVERY);
            case QUALITY_CLAIM, LAB_RESULT -> List.of(UploadStage.CLAIM);
        };
    }
}
