package com.yarnshipment.discrepancy.domain.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fabrika personelinin (kalite, müdür, admin) tamamlanmış sevkiyat için kilogram bazlı açtığı kalite itirazı.
 * Her itiraz bir {@code QUALITY} uyuşmazlığıdır; inceleme ve karar o uyuşmazlık üzerinden yürür. İtiraz açıldıktan
 * sonra değişmez.
 *
 * @param id kimlik
 * @param discrepancyId itirazın uyuşmazlığı
 * @param shipmentId sevkiyat
 * @param subcontractorId sevkiyatın fasoncusu (ana veri); bilinmiyorsa boş
 * @param opening kim, ne zaman ve hangi süre içinde açtı
 * @param description açıklama
 * @param claimedKg itiraz edilen kilo; çuval bazlı eski itirazlarda sıfır
 * @param fabricReceiptId isteğe bağlı kumaş girişi kimliği
 * @param evidenceIds itiraz fotoğrafları
 */
public record QualityClaim(
        UUID id,
        UUID discrepancyId,
        UUID shipmentId,
        @Nullable UUID subcontractorId,
        Opening opening,
        String description,
        BigDecimal claimedKg,
        @Nullable UUID fabricReceiptId,
        List<UUID> evidenceIds) {

    public QualityClaim {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(discrepancyId, "discrepancyId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(opening, "opening");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(claimedKg, "claimedKg");
        evidenceIds = List.copyOf(evidenceIds);
    }

    /**
     * İtirazın açılışı.
     *
     * @param openedBy açan kullanıcı
     * @param openedAt açılış zamanı
     * @param deadlineAt itiraz süresinin sonu
     */
    public record Opening(UUID openedBy, Instant openedAt, Instant deadlineAt) {

        public Opening {
            Objects.requireNonNull(openedBy, "openedBy");
            Objects.requireNonNull(openedAt, "openedAt");
            Objects.requireNonNull(deadlineAt, "deadlineAt");
        }
    }
}
