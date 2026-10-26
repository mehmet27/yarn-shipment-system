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
 * @param siteId çıkış lokasyonu
 * @param shipmentId sevkiyat
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
        UUID siteId,
        UUID shipmentId,
        Basis basis,
        Measure measure,
        DiscrepancyStatus status,
        Handling handling,
        @Nullable Decision decision,
        Instant createdAt) {

    public Discrepancy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(discrepancyNo, "discrepancyNo");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(shipmentId, "shipmentId");
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
                siteId,
                shipmentId,
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
                siteId,
                shipmentId,
                basis,
                measure,
                DiscrepancyStatus.RESOLVED,
                handling,
                resolvedWith,
                createdAt);
    }

    private void requireUnresolved() {
        if (!status.isUnresolved()) {
            throw new DiscrepancyNotOpenException(id, status);
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
