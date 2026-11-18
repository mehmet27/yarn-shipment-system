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
 * @param receivedKg sevkiyata girilmiş kumaşın toplam net kilosu; kumaş girişi yoksa sıfır
 * @param claimedKg haksız bulunmamış (açık ya da haklı) itirazların toplam kilosu
 */
public record ClaimTarget(
        UUID shipmentId,
        UUID siteId,
        @Nullable UUID subcontractorId,
        @Nullable String shipmentNo,
        Instant completedAt,
        BigDecimal receivedKg,
        BigDecimal claimedKg) {

    /**
     * Hedef süre: tamamlanmadan itibaren 3 iş günü. İtiraz açmak için süre sınırı yoktur (iş birimi kararı, akış
     * dokümanı S18); bu tarih yalnızca bilgi olarak gösterilir.
     */
    public static final int CLAIM_BUSINESS_DAYS = 3;

    /** İş günü hesabı fabrikanın saat dilimiyle yapılır. */
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    public ClaimTarget {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(receivedKg, "receivedKg");
        Objects.requireNonNull(claimedKg, "claimedKg");
    }

    /** İtiraz edilebilecek kilo: girilmiş kumaştan, daha önce itiraz edilen kilo çıkarılır; sıfırın altına inmez. */
    public BigDecimal claimableKg() {
        return receivedKg.subtract(claimedKg).max(BigDecimal.ZERO);
    }

    /** Hedef tarih; itirazın açılmasını sınırlamaz. */
    public Instant deadline() {
        return BusinessDays.plus(completedAt, CLAIM_BUSINESS_DAYS, FACTORY_ZONE);
    }

    /** İtiraz süresi sınırsızdır; tamamlanmış sevkiyata her zaman itiraz açılabilir. */
    public boolean isOpenAt(Instant now) {
        return true;
    }

    /** Kilo ve kanıt kurallarını denetler; tutmayan ilk kural için {@link ClaimRejectedException} fırlatır. */
    public void requireClaimable(ClaimRequest request, Instant now) {
        if (!isOpenAt(now)) {
            throw new ClaimRejectedException(ClaimRejection.WINDOW_CLOSED, "shipmentId=" + shipmentId);
        }
        if (receivedKg.signum() == 0) {
            throw new ClaimRejectedException(ClaimRejection.FABRIC_NOT_RECEIVED, "shipmentId=" + shipmentId);
        }
        if (request.claimedKg().compareTo(claimableKg()) > 0) {
            throw new ClaimRejectedException(ClaimRejection.CLAIM_EXCEEDS_SHIPMENT_WEIGHT, "shipmentId=" + shipmentId);
        }
        if (request.evidenceIds().isEmpty()) {
            throw new ClaimRejectedException(ClaimRejection.EVIDENCE_REQUIRED, "shipmentId=" + shipmentId);
        }
    }
}
