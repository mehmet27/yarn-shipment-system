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
 * @param actorName işlemi yapanın adı; çözülemediyse boş
 * @param assigneeName atamada atanan kişinin adı; atama değilse ya da çözülemediyse boş
 */
public record DiscrepancyActivity(
        UUID id,
        String actionType,
        @Nullable String note,
        @Nullable UUID evidenceId,
        UUID actorId,
        Instant occurredAt,
        @Nullable String actorName,
        @Nullable String assigneeName) {

    public DiscrepancyActivity(
            UUID id,
            String actionType,
            @Nullable String note,
            @Nullable UUID evidenceId,
            UUID actorId,
            Instant occurredAt) {
        this(id, actionType, note, evidenceId, actorId, occurredAt, null, null);
    }

    /** Aynı kayıt, çözülen adlarla. */
    public DiscrepancyActivity withNames(@Nullable String actor, @Nullable String assignee) {
        return new DiscrepancyActivity(id, actionType, note, evidenceId, actorId, occurredAt, actor, assignee);
    }
}
