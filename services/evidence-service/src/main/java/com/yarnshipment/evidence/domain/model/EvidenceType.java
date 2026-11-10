package com.yarnshipment.evidence.domain.model;

import java.util.List;

/** Kanıt türleri (veritabanındaki {@code evidence_type} kısıtıyla aynı) ve yüklenebildikleri aşamalar. */
public enum EvidenceType {
    DISPATCH_SCALE,
    BAG_OVERVIEW,
    BAG_LABEL,
    /** Müdürün tamamlamada yüklediği irsaliye. */
    WAYBILL,
    QUALITY_CLAIM,
    LAB_RESULT;

    /** Bu tür hangi aşamalardaki izinlerle yüklenebilir. */
    public List<UploadStage> stages() {
        return switch (this) {
            case DISPATCH_SCALE, BAG_OVERVIEW, BAG_LABEL -> List.of(UploadStage.DISPATCH);
            case WAYBILL -> List.of(UploadStage.COMPLETION);
            case QUALITY_CLAIM, LAB_RESULT -> List.of(UploadStage.CLAIM);
        };
    }
}
