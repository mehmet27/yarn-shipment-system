package com.yarnshipment.discrepancy.domain.claim;

import com.yarnshipment.discrepancy.domain.model.BusinessDays;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İtiraz açılabilecek tamamlanmış sevkiyat: sevkiyat olaylarından tutulan izleme kaydından okunur.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param subcontractorId sevkiyatın fasoncusu (ana veri); bilinmiyorsa boş
 * @param shipmentNo sevkiyat numarası; uyuşmazlık numarasında kullanılır
 * @param completedAt sevkiyatın tamamlanma zamanı; itiraz süresi buradan sayılır
 * @param netKg sevkiyatın çıkış net kilosu; itiraz edilen kilo bunu aşamaz
 */
public record ClaimTarget(
        UUID shipmentId,
        UUID siteId,
        @Nullable UUID subcontractorId,
        @Nullable String shipmentNo,
        Instant completedAt,
        BigDecimal netKg) {

    /** İtiraz süresi: tamamlanmadan itibaren 3 iş günü (analiz v2.0, 5.12). */
    public static final int CLAIM_BUSINESS_DAYS = 3;

    /** İş günü hesabı fabrikanın saat dilimiyle yapılır. */
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    public ClaimTarget {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(netKg, "netKg");
    }

    /** İtirazın açılabileceği son an. */
    public Instant deadline() {
        return BusinessDays.plus(completedAt, CLAIM_BUSINESS_DAYS, FACTORY_ZONE);
    }

    public boolean isOpenAt(Instant now) {
        return !now.isAfter(deadline());
    }

    /** Süre, kilo ve kanıt kurallarını denetler; tutmayan ilk kural için {@link ClaimRejectedException} fırlatır. */
    public void requireClaimable(ClaimRequest request, Instant now) {
        if (!isOpenAt(now)) {
            throw new ClaimRejectedException(ClaimRejection.WINDOW_CLOSED, "shipmentId=" + shipmentId);
        }
        if (request.claimedKg().compareTo(netKg) > 0) {
            throw new ClaimRejectedException(ClaimRejection.CLAIM_EXCEEDS_SHIPMENT_WEIGHT, "shipmentId=" + shipmentId);
        }
        if (request.evidenceIds().isEmpty()) {
            throw new ClaimRejectedException(ClaimRejection.EVIDENCE_REQUIRED, "shipmentId=" + shipmentId);
        }
    }
}
