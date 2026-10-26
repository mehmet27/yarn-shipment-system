package com.yarnshipment.discrepancy.domain.model;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Uyuşmazlık geçmişindeki bir kayıt.
 *
 * @param id kimlik
 * @param actionType {@code OPENED}, {@code ASSIGNED} ya da {@code RESOLVED}
 * @param note not
 * @param evidenceId ekli kanıt
 * @param actorId işlemi yapan; otomatik karşılaştırmada sistem hesabı
 * @param occurredAt zaman
 */
public record DiscrepancyActivity(
        UUID id,
        String actionType,
        @Nullable String note,
        @Nullable UUID evidenceId,
        UUID actorId,
        Instant occurredAt) {}
