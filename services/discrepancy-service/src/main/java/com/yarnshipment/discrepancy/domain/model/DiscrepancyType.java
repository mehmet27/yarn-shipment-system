package com.yarnshipment.discrepancy.domain.model;

/** Uyuşmazlık türleri (ön analiz, Bölüm 12.1). Karşılaştırma QUANTITY, BAG, SEAL ve DOCUMENT üretir. */
public enum DiscrepancyType {
    QUANTITY,
    BAG,
    SEAL,
    QUALITY,
    DOCUMENT,
    PHOTO,
    TIME
}
