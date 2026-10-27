package com.yarnshipment.events.discrepancy;

import java.time.Instant;
import java.util.UUID;

/**
 * UyusmazlikCozuldu: bir uyuşmazlık karara bağlandı. Sevkiyatın açık uyuşmazlığı kalmadıysa sevkiyat tamamlanır.
 *
 * @param discrepancyId uyuşmazlık
 * @param discrepancyNo okunabilir numara
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param resolution çözüm kodu, ör. {@code TOLERANCE}
 * @param resolvedAt karar zamanı
 * @param remainingOpen aynı sevkiyatta henüz çözülmemiş uyuşmazlık sayısı
 */
public record DiscrepancyResolved(
        UUID discrepancyId,
        String discrepancyNo,
        UUID shipmentId,
        UUID siteId,
        String resolution,
        Instant resolvedAt,
        int remainingOpen) {

    public static final String TYPE = "com.yarnshipment.discrepancy.discrepancy-resolved.v1";

    public boolean allResolved() {
        return remainingOpen == 0;
    }
}
