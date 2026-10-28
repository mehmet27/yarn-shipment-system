package com.yarnshipment.evidence;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.ticket.ReadConfidence;
import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketFields;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import com.yarnshipment.evidence.domain.ticket.WeightValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Testlerde kullanılan hazır fiş okumaları. */
public final class TestTickets {

    public static final ReaderVersion VERSION = new ReaderVersion("claude-sonnet-5-5", "ticket-v1");
    public static final BigDecimal GROSS = new BigDecimal("12480");
    public static final BigDecimal TARE = new BigDecimal("8210");
    public static final BigDecimal NET = new BigDecimal("4270");
    public static final String PLATE = "34 ABC 123";
    public static final String RAW = "{\"gross\":{}}";

    private TestTickets() {}

    /** Verilen kişinin yüklediği, verilen türdeki kanıt. */
    public static EvidenceFile file(UUID uploadedBy, EvidenceType type) {
        UploadIntent base = TestUploads.intent(uploadedBy);
        UploadIntent intent = new UploadIntent(
                base.id(),
                base.shipmentId(),
                base.siteId(),
                TestUploads.declared(type),
                base.storageKey(),
                base.requestedBy(),
                base.requestedAt(),
                base.expiresAt());
        return EvidenceFile.confirm(intent, TestUploads.stored(), TestUploads.NOW);
    }

    public static WeightValue trusted(BigDecimal kg) {
        return new WeightValue(kg, ReadConfidence.HIGH);
    }

    public static TicketScan scan(WeightValue gross, WeightValue tare, WeightValue net) {
        return new TicketScan(gross, tare, net, fields(TestUploads.NOW), List.of(), RAW);
    }

    /** Brüt ve darası okunmuş, tarihi ve fotoğraf sorunları verilen fiş. */
    public static TicketScan scanAt(@Nullable Instant weighedAt, List<ReadingProblem> photoProblems) {
        return new TicketScan(
                trusted(GROSS), trusted(TARE), WeightValue.MISSING, fields(weighedAt), photoProblems, RAW);
    }

    private static TicketFields fields(@Nullable Instant weighedAt) {
        return new TicketFields(PLATE, weighedAt, "004512", "Kantar A.Ş.");
    }

    /** Brüt, dara ve neti tutarlı, net okunmuş fiş. */
    public static TicketScan clearScan() {
        return scan(trusted(GROSS), trusted(TARE), trusted(NET));
    }
}
