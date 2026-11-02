package com.yarnshipment.discrepancy.domain.model;

/**
 * Uyuşmazlık türleri (ön analiz, Bölüm 12.1). Karşılaştırma QUANTITY, BAG, SEAL, DOCUMENT ve eksik fotoğraf için
 * PHOTO üretir; tekrar kullanılan fotoğraf (PHOTO) ve yolda süre aşımı (TIME) olaylardan ve zamanlanmış kontrolden
 * gelir.
 */
public enum DiscrepancyType {
    QUANTITY,
    BAG,
    SEAL,
    QUALITY,
    DOCUMENT,
    PHOTO,
    TIME
}
