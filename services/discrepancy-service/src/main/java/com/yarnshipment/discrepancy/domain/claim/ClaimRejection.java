package com.yarnshipment.discrepancy.domain.claim;

/** Kalite itirazı ya da kontrol isteğinin reddedilme nedeni; problem kodu {@code QUALITY_CLAIM_<ad>} olur. */
public enum ClaimRejection {
    /** Sevkiyat tamamlanmadı ya da çıkış kilosu bilinmiyor. */
    NOT_AVAILABLE,
    /** Teslimden sonraki itiraz süresi doldu. */
    WINDOW_CLOSED,
    /** İtiraz edilen kilo sevkiyatın net kilosunu aşıyor. */
    CLAIM_EXCEEDS_SHIPMENT_WEIGHT,
    /** İtiraz edilen kilo sıfırdan büyük ve en çok üç ondalıklı olmalı. */
    CLAIMED_KG_INVALID,
    /** İtiraz fotoğrafsız açılamaz. */
    EVIDENCE_REQUIRED,
    /** Kanıt bulunamadı, başka sevkiyatın ya da beklenen türde değil. */
    EVIDENCE_INVALID,
    /** Kontrol sonucu zaten girildi. */
    INSPECTION_RECORDED,
    /** Lab sonucu belgesiz girilemez. */
    RESULT_EVIDENCE_REQUIRED,
    INSPECTION_PENDING
}
