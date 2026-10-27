package com.yarnshipment.events.discrepancy;

import java.time.Instant;
import java.util.UUID;

/**
 * UyusmazlikAcildi: karşılaştırma bir uyuşmazlık açtı. Kişisel veri taşımaz (EVT-08).
 *
 * @param discrepancyId uyuşmazlık
 * @param discrepancyNo okunabilir numara
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param type uyuşmazlık türü, ör. {@code QUANTITY}
 * @param severity {@code YELLOW} ya da {@code RED}
 * @param ruleCode ihlal edilen kural
 * @param dueAt çözüm vadesi
 */
public record DiscrepancyOpened(
        UUID discrepancyId,
        String discrepancyNo,
        UUID shipmentId,
        UUID siteId,
        String type,
        String severity,
        String ruleCode,
        Instant dueAt) {

    public static final String TYPE = "com.yarnshipment.discrepancy.discrepancy-opened.v1";
}
