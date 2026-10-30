package com.yarnshipment.discrepancy.domain.claim;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * İtirazı açanın girdiği bilgiler.
 *
 * @param description itirazın açıklaması
 * @param bagKeys itiraz edilen çuvalların anahtarları; tekrarlar ayıklanır
 * @param evidenceIds itiraz fotoğrafları ({@code QUALITY_CLAIM} kanıtları)
 */
public record ClaimRequest(String description, List<String> bagKeys, List<UUID> evidenceIds) {

    public ClaimRequest {
        Objects.requireNonNull(description, "description");
        bagKeys = bagKeys.stream().map(String::strip).distinct().toList();
        evidenceIds = evidenceIds.stream().distinct().toList();
    }
}
