package com.yarnshipment.discrepancy.domain.claim;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fasoncunun (ya da kalite/admin'in fasoncu adına) açtığı kalite itirazı. Her itiraz bir {@code QUALITY}
 * uyuşmazlığıdır; inceleme ve karar o uyuşmazlık üzerinden yürür. İtiraz açıldıktan sonra değişmez.
 *
 * @param id kimlik
 * @param discrepancyId itirazın uyuşmazlığı
 * @param shipmentId sevkiyat
 * @param subcontractorId sevkiyatın fasoncusu
 * @param opening kim, ne zaman ve hangi süre içinde açtı
 * @param description açıklama
 * @param bagKeys itiraz edilen çuvallar
 * @param evidenceIds itiraz fotoğrafları
 */
public record QualityClaim(
        UUID id,
        UUID discrepancyId,
        UUID shipmentId,
        UUID subcontractorId,
        Opening opening,
        String description,
        List<String> bagKeys,
        List<UUID> evidenceIds) {

    public QualityClaim {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(discrepancyId, "discrepancyId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(opening, "opening");
        Objects.requireNonNull(description, "description");
        bagKeys = List.copyOf(bagKeys);
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
