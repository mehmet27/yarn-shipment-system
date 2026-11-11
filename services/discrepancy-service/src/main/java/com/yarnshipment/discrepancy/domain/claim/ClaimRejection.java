package com.yarnshipment.discrepancy.domain.claim;

/** Kalite itirazı ya da kontrol isteğinin reddedilme nedeni; problem kodu {@code QUALITY_CLAIM_<ad>} olur. */
public enum ClaimRejection {
    /** Sevkiyat henüz karşılaştırılmadı ya da karşılaştırma girdisi lokasyon ve fasoncuyu taşımıyor. */
    NOT_AVAILABLE,
    /** Teslimden sonraki itiraz süresi doldu. */
    WINDOW_CLOSED,
    /** İtiraz edilen anahtar teslimde okutulmamış. */
    BAG_NOT_DELIVERED,
    /** Çuval aynı sevkiyatta daha önce itiraz edildi. */
    BAG_ALREADY_CLAIMED,
    /** İtiraz fotoğrafsız açılamaz. */
    EVIDENCE_REQUIRED,
    /** Kanıt bulunamadı, başka sevkiyatın ya da beklenen türde değil. */
    EVIDENCE_INVALID,
    /** Kontrol sonucu zaten girildi. */
    INSPECTION_RECORDED,
    /** Lab sonucu belgesiz girilemez. */
    RESULT_EVIDENCE_REQUIRED
}
