package com.yarnshipment.discrepancy.domain.claim;

/** Kalite itirazının reddedilme nedeni; problem kodu {@code QUALITY_CLAIM_<ad>} olur. */
public enum ClaimRejection {
    /** Sevkiyat tamamlanmadı ya da çıkış kilosu bilinmiyor. */
    NOT_AVAILABLE,
    /** Teslimden sonraki itiraz süresi doldu. */
    WINDOW_CLOSED,
    /** Sevkiyata henüz kumaş girişi yapılmadı; girilmemiş kumaşa itiraz açılamaz. */
    FABRIC_NOT_RECEIVED,
    /** İtiraz edilen kilo, girilmiş kumaştan daha önce itiraz edilen kilo çıkarılınca kalanı aşıyor. */
    CLAIM_EXCEEDS_SHIPMENT_WEIGHT,
    /** İtiraz edilen kilo sıfırdan büyük ve en çok üç ondalıklı olmalı. */
    CLAIMED_KG_INVALID,
    /** İtiraz açıklaması boş olamaz. */
    DESCRIPTION_REQUIRED,
    /** İtiraz fotoğrafsız açılamaz. */
    EVIDENCE_REQUIRED,
    /** Kanıt bulunamadı, başka sevkiyatın ya da beklenen türde değil. */
    EVIDENCE_INVALID
}
