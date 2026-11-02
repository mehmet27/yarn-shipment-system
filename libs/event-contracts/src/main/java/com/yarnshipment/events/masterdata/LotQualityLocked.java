package com.yarnshipment.events.masterdata;

import java.time.Instant;
import java.util.UUID;

/**
 * PartiKalitesiKilitlendi: partinin kalitesi kilitlendi, artık değişmez (rapor Bölüm 10). bag-service bu olayla
 * çuvalın bağlanabileceği partileri ve kalitelerini öğrenir.
 *
 * @param lotId parti
 * @param siteId partinin lokasyonu
 * @param lotNo parti numarası
 * @param productId iplik / ürün
 * @param qualityGradeCode kalite sınıfı kodu; çuvala kopyalanır
 * @param lockedAt kilitlenme zamanı
 */
public record LotQualityLocked(
        UUID lotId, UUID siteId, String lotNo, UUID productId, String qualityGradeCode, Instant lockedAt) {

    public static final String TYPE = "com.yarnshipment.masterdata.lot-quality-locked.v1";
}
