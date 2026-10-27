package com.yarnshipment.evidence.domain.ticket;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kantar fişinin güven kurallarından geçmiş okuması. Kilolar yalnızca {@link TicketReadingStatus#READ} iken
 * doludur. Kayıt değişmez; yeniden çekim yeni bir kanıt ve yeni bir okumadır.
 *
 * @param evidenceId okunan kanıt
 * @param shipmentId sevkiyat
 * @param evidenceType {@code DISPATCH_SCALE} veya {@code DELIVERY_SCALE}
 * @param status sonuç
 * @param weights güvenle okunmuş brüt ve dara; okunamadıysa null
 * @param fields kilo dışındaki alanlar
 * @param problems görülen sorunlar
 * @param version okuyan model ve istem sürümü
 * @param rawResponse modelin ham cevabı
 * @param readAt okuma zamanı
 */
public record TicketReading(
        UUID evidenceId,
        UUID shipmentId,
        EvidenceType evidenceType,
        TicketReadingStatus status,
        @Nullable ReadWeights weights,
        TicketFields fields,
        List<ReadingProblem> problems,
        ReaderVersion version,
        String rawResponse,
        Instant readAt) {

    /** Fişteki net ile brüt eksi dara arasındaki en büyük kabul edilen fark. */
    static final BigDecimal ARITHMETIC_TOLERANCE_KG = BigDecimal.ONE;

    /** Fiş tarihinin yükleme zamanından en fazla sapması. */
    static final Duration DATE_TOLERANCE = Duration.ofDays(2);

    private static final Set<EvidenceType> SCALE_TICKETS =
            Set.of(EvidenceType.DISPATCH_SCALE, EvidenceType.DELIVERY_SCALE);

    public TicketReading {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(evidenceType, "evidenceType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(fields, "fields");
        problems = List.copyOf(problems);
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(rawResponse, "rawResponse");
        Objects.requireNonNull(readAt, "readAt");
        if ((status == TicketReadingStatus.READ) != (weights != null)) {
            throw new IllegalArgumentException("Weights must be present exactly when the ticket is read");
        }
    }

    /** Bu tür kanıt okunabilir mi. */
    public static boolean isReadable(EvidenceType type) {
        return SCALE_TICKETS.contains(type);
    }

    /**
     * Modelin okumasını sunucu kurallarından geçirir; model ne derse desin kurallar burada uygulanır.
     *
     * <ol>
     *   <li>Brüt ve dara okunmuş ve model emin olmalı.
     *   <li>Dara brütten küçük olmalı.
     *   <li>Fişte net basılıysa brüt eksi dara ile en fazla 1 kg farklı olabilir.
     *   <li>Fiş tarihi yüklemeden 48 saatten fazla sapıyorsa uyarı yazılır, okuma yine geçerlidir.
     * </ol>
     */
    public static TicketReading evaluate(EvidenceFile file, TicketScan scan, ReaderVersion version, Instant at) {
        if (!isReadable(file.declared().type())) {
            throw new TicketReadingNotSupportedException(file.id());
        }
        List<ReadingProblem> problems = problemsOf(scan, file.uploadedAt());
        boolean read = problems.stream().noneMatch(ReadingProblem::blocksReading);
        BigDecimal gross = scan.gross().kg();
        BigDecimal tare = scan.tare().kg();
        ReadWeights weights = read && gross != null && tare != null ? new ReadWeights(gross, tare) : null;
        return new TicketReading(
                file.id(),
                file.shipmentId(),
                file.declared().type(),
                weights == null ? TicketReadingStatus.UNREADABLE : TicketReadingStatus.READ,
                weights,
                scan.fields(),
                problems,
                version,
                scan.rawResponse(),
                at);
    }

    private static List<ReadingProblem> problemsOf(TicketScan scan, Instant uploadedAt) {
        List<ReadingProblem> problems = new ArrayList<>();
        ReadingProblem weightProblem = weightProblem(scan);
        if (weightProblem != null) {
            problems.add(weightProblem);
        }
        Instant weighedAt = scan.fields().weighedAt();
        if (weighedAt != null && Duration.between(weighedAt, uploadedAt).abs().compareTo(DATE_TOLERANCE) > 0) {
            problems.add(ReadingProblem.DATE_OUT_OF_RANGE);
        }
        scan.photoProblems().stream()
                .filter(problem -> !problems.contains(problem))
                .forEach(problems::add);
        return problems;
    }

    private static @Nullable ReadingProblem weightProblem(TicketScan scan) {
        BigDecimal gross = scan.gross().kg();
        BigDecimal tare = scan.tare().kg();
        BigDecimal printedNet = scan.net().kg();
        ReadingProblem problem = null;
        if (gross == null
                || tare == null
                || !scan.gross().isTrusted()
                || !scan.tare().isTrusted()) {
            problem = ReadingProblem.WEIGHT_NOT_READ;
        } else if (tare.signum() < 0 || tare.compareTo(gross) >= 0) {
            problem = ReadingProblem.WEIGHT_INCONSISTENT;
        } else if (printedNet != null
                && gross.subtract(tare).subtract(printedNet).abs().compareTo(ARITHMETIC_TOLERANCE_KG) > 0) {
            problem = ReadingProblem.ARITHMETIC_MISMATCH;
        }
        return problem;
    }
}
