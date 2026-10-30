package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir sevkiyatta açılmış uyuşmazlık.
 *
 * @param id kimlik
 * @param discrepancyNo okunabilir numara
 * @param origin lokasyon, sevkiyat ve fasoncu
 * @param basis neden açıldı
 * @param measure beklenen ve gerçek değer
 * @param status durum
 * @param handling sorumlu ve vade
 * @param decision karar; çözülmemişse boş
 * @param createdAt açılış zamanı
 */
public record Discrepancy(
        UUID id,
        String discrepancyNo,
        Origin origin,
        Basis basis,
        Measure measure,
        DiscrepancyStatus status,
        Handling handling,
        @Nullable Decision decision,
        Instant createdAt) {

    public Discrepancy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(discrepancyNo, "discrepancyNo");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(measure, "measure");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(handling, "handling");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Sorumluya atar ve incelemeye alır; yeniden atama sorumluyu değiştirir. */
    public Discrepancy assignTo(UUID assigneeId) {
        requireUnresolved();
        return new Discrepancy(
                id,
                discrepancyNo,
                origin,
                basis,
                measure,
                DiscrepancyStatus.IN_REVIEW,
                new Handling(assigneeId, handling.dueAt()),
                null,
                createdAt);
    }

    /** Karara bağlar. */
    public Discrepancy resolve(Decision resolvedWith) {
        requireUnresolved();
        if (resolvedWith.resolution() == Resolution.LIABILITY_ASSIGNED && resolvedWith.liableParty() == null) {
            throw new LiablePartyRequiredException();
        }
        return new Discrepancy(
                id,
                discrepancyNo,
                origin,
                basis,
                measure,
                DiscrepancyStatus.RESOLVED,
                handling,
                resolvedWith,
                createdAt);
    }

    public UUID siteId() {
        return origin.siteId();
    }

    public UUID shipmentId() {
        return origin.shipmentId();
    }

    public @Nullable UUID subcontractorId() {
        return origin.subcontractorId();
    }

    private void requireUnresolved() {
        if (!status.isUnresolved()) {
            throw new DiscrepancyNotOpenException(id, status);
        }
    }

    /**
     * Uyuşmazlığın ait olduğu yer.
     *
     * @param siteId çıkış lokasyonu
     * @param shipmentId sevkiyat
     * @param subcontractorId sevkiyatın fasoncusu; fasoncu yalnızca kendi uyuşmazlıklarını görür. Fasoncu bilgisi
     *     olaydan gelmeye başlamadan önce açılmış kayıtlarda boştur ve bunlar fasoncuya görünmez
     */
    public record Origin(
            UUID siteId, UUID shipmentId, @Nullable UUID subcontractorId) {

        public Origin {
            Objects.requireNonNull(siteId, "siteId");
            Objects.requireNonNull(shipmentId, "shipmentId");
        }
    }

    /**
     * Uyuşmazlığın nedeni.
     *
     * @param type tür
     * @param severity öncelik
     * @param ruleCode ihlal edilen kural
     */
    public record Basis(DiscrepancyType type, Severity severity, String ruleCode) {}

    /**
     * Karşılaştırılan değerler.
     *
     * @param expected beklenen değer
     * @param actual gerçek değer
     * @param diffKg kilo farkı
     * @param diffPct yüzde fark
     */
    public record Measure(
            @Nullable String expected,
            @Nullable String actual,
            @Nullable BigDecimal diffKg,
            @Nullable BigDecimal diffPct) {}

    /**
     * Kimde ve ne zamana kadar.
     *
     * @param assigneeId sorumlu; atanmamışsa boş
     * @param dueAt çözüm vadesi
     */
    public record Handling(
            @Nullable UUID assigneeId, @Nullable Instant dueAt) {}

    /**
     * Verilen karar.
     *
     * @param resolution karar kodu
     * @param note gerekçe
     * @param liableParty sorumlu taraf; yalnızca sorumluluk atanan kararda
     * @param resolvedBy karar veren
     * @param resolvedAt karar zamanı
     */
    public record Decision(
            Resolution resolution, String note, @Nullable String liableParty, UUID resolvedBy, Instant resolvedAt) {

        public Decision {
            Objects.requireNonNull(resolution, "resolution");
            Objects.requireNonNull(note, "note");
            Objects.requireNonNull(resolvedBy, "resolvedBy");
            Objects.requireNonNull(resolvedAt, "resolvedAt");
        }
    }
}
