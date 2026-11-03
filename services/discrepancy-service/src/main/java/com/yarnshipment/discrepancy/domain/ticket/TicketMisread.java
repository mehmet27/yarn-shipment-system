package com.yarnshipment.discrepancy.domain.ticket;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İnceleyen kişi fiş okumasının yanlış olduğunu işaretledi ("AI yanıldı"); isabet ölçümüne girer. Okuma başına tek
 * kayıt, değişmez.
 *
 * @param evidenceId okunan fotoğraf
 * @param discrepancyId işaretin konduğu uyuşmazlık
 * @param markedBy işaretleyen
 * @param markedAt işaret zamanı
 * @param note isteğe bağlı açıklama, ör. "brüt 12 480, 12 430 okunmuş"
 */
public record TicketMisread(
        UUID evidenceId,
        UUID discrepancyId,
        UUID markedBy,
        Instant markedAt,
        @Nullable String note) {

    public TicketMisread {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(discrepancyId, "discrepancyId");
        Objects.requireNonNull(markedBy, "markedBy");
        Objects.requireNonNull(markedAt, "markedAt");
    }
}
