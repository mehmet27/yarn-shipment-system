package com.yarnshipment.discrepancy.domain.claim;

import com.yarnshipment.discrepancy.domain.model.BusinessDays;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İtiraz açılabilecek teslim: karşılaştırma girdisinden okunur.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param subcontractorId sevkiyatın fasoncusu
 * @param shipmentNo sevkiyat numarası; uyuşmazlık numarasında kullanılır
 * @param receivedAt teslim girişinin zamanı; itiraz süresi buradan sayılır
 * @param deliveredKeys teslimde okutulan anahtarlar
 */
public record ClaimTarget(
        UUID shipmentId,
        UUID siteId,
        UUID subcontractorId,
        @Nullable String shipmentNo,
        Instant receivedAt,
        List<String> deliveredKeys) {

    /** İtiraz süresi: teslimden itibaren 3 iş günü (ön analiz, karar #7). */
    public static final int CLAIM_BUSINESS_DAYS = 3;

    /** İş günü hesabı fabrikanın saat dilimiyle yapılır. */
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    public ClaimTarget {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(receivedAt, "receivedAt");
        deliveredKeys = List.copyOf(deliveredKeys);
    }

    /** İtirazın açılabileceği son an. */
    public Instant deadline() {
        return BusinessDays.plus(receivedAt, CLAIM_BUSINESS_DAYS, FACTORY_ZONE);
    }

    public boolean isOpenAt(Instant now) {
        return !now.isAfter(deadline());
    }

    /**
     * Süre, çuval ve kanıt kurallarını denetler; tutmayan ilk kural için {@link ClaimRejectedException} fırlatır.
     *
     * @param claimedKeys sevkiyatta daha önce itiraz edilmiş anahtarlar
     */
    public void requireClaimable(ClaimRequest request, Collection<String> claimedKeys, Instant now) {
        if (!isOpenAt(now)) {
            throw new ClaimRejectedException(ClaimRejection.WINDOW_CLOSED, "shipmentId=" + shipmentId);
        }
        if (request.bagKeys().isEmpty() || !deliveredKeys.containsAll(request.bagKeys())) {
            throw new ClaimRejectedException(ClaimRejection.BAG_NOT_DELIVERED, "shipmentId=" + shipmentId);
        }
        if (request.bagKeys().stream().anyMatch(claimedKeys::contains)) {
            throw new ClaimRejectedException(ClaimRejection.BAG_ALREADY_CLAIMED, "shipmentId=" + shipmentId);
        }
        if (request.evidenceIds().isEmpty()) {
            throw new ClaimRejectedException(ClaimRejection.EVIDENCE_REQUIRED, "shipmentId=" + shipmentId);
        }
    }
}
