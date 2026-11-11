package com.yarnshipment.events.discrepancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
 * @param claimedKg haklı bulunan kalite itirazının kilosu ({@code CLAIM_JUSTIFIED}); başka kararlarda boş. Bakiye
 *     defteri bu kiloyu iplikten düşer (KR-15)
 */
public record DiscrepancyResolved(
        UUID discrepancyId,
        String discrepancyNo,
        UUID shipmentId,
        UUID siteId,
        String resolution,
        Instant resolvedAt,
        int remainingOpen,
        @Nullable BigDecimal claimedKg) {

    public static final String TYPE = "com.yarnshipment.discrepancy.discrepancy-resolved.v1";

    public boolean allResolved() {
        return remainingOpen == 0;
    }
}
