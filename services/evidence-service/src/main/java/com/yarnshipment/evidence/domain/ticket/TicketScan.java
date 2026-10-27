package com.yarnshipment.evidence.domain.ticket;

import java.util.List;
import java.util.Objects;

/**
 * Modelin fişten okuduğu ham alanlar; henüz güven kurallarından geçmemiştir.
 *
 * @param gross brüt
 * @param tare dara
 * @param net fişte basılı net; yoksa {@link WeightValue#MISSING}
 * @param fields kilo dışındaki alanlar
 * @param photoProblems modelin fotoğrafta gördüğü sorunlar
 * @param rawResponse modelin ham cevabı; denetim için saklanır
 */
public record TicketScan(
        WeightValue gross,
        WeightValue tare,
        WeightValue net,
        TicketFields fields,
        List<ReadingProblem> photoProblems,
        String rawResponse) {

    public TicketScan {
        Objects.requireNonNull(gross, "gross");
        Objects.requireNonNull(tare, "tare");
        Objects.requireNonNull(net, "net");
        Objects.requireNonNull(fields, "fields");
        photoProblems = List.copyOf(photoProblems);
        Objects.requireNonNull(rawResponse, "rawResponse");
    }

    /** Görüntü modele gönderilemeden okunamadı (ör. desteklenmeyen biçim). */
    public static TicketScan unreadableImage(ReadingProblem problem) {
        return new TicketScan(
                WeightValue.MISSING,
                WeightValue.MISSING,
                WeightValue.MISSING,
                TicketFields.EMPTY,
                List.of(problem),
                "{}");
    }
}
