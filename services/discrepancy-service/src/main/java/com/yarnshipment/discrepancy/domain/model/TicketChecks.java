package com.yarnshipment.discrepancy.domain.model;

import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketReuse;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.discrepancy.domain.ticket.WeighingEntries;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Kantar fişi kontrolleri (tasarım K1–K5). Evet/hayır kararını fişi okuyan model değil, bu kurallar ve tolerans
 * bantları verir; okunmamış fişin kontrolü atlanır, sevkiyatı durdurmaz.
 */
public final class TicketChecks {

    /** K1: depo girişi ↔ çıkış fişi. */
    public static final String DISPATCH_WEIGHT_MISMATCH = "TICKET_DISPATCH_WEIGHT_MISMATCH";

    /** K2: fasoncu girişi ↔ teslim fişi. */
    public static final String DELIVERY_WEIGHT_MISMATCH = "TICKET_DELIVERY_WEIGHT_MISMATCH";

    /** K3: çıkış fişi neti ↔ teslim fişi neti. */
    public static final String QUANTITY_TOLERANCE = "TICKET_QUANTITY_TOLERANCE";

    /** Aynı fişin rakamı karşılaştırıldığı için yalnızca okuma ve yuvarlama payı vardır. */
    private static final BigDecimal SAME_TICKET_KG = BigDecimal.ONE;

    private static final Pattern PLATE_NOISE = Pattern.compile("[\\s-]");

    private TicketChecks() {}

    public static List<Finding> compare(WeighingEntries entries, ShipmentTickets tickets, ToleranceBands bands) {
        List<Finding> findings = new ArrayList<>();
        addIfFound(
                findings, compareEntry(DISPATCH_WEIGHT_MISMATCH, entries.dispatch(), tickets.dispatchWeights(), bands));
        addIfFound(
                findings, compareEntry(DELIVERY_WEIGHT_MISMATCH, entries.delivery(), tickets.deliveryWeights(), bands));
        compareTickets(tickets, bands, findings);
        comparePlate(ComparisonRule.TICKET_DISPATCH_PLATE_MISMATCH, entries.plateNo(), tickets.dispatch(), findings);
        comparePlate(ComparisonRule.TICKET_DELIVERY_PLATE_MISMATCH, entries.plateNo(), tickets.delivery(), findings);
        for (TicketReuse reuse : tickets.reuses()) {
            ComparisonRule rule = reuse.side() == TicketSide.DISPATCH_SCALE
                    ? ComparisonRule.TICKET_DISPATCH_REUSED
                    : ComparisonRule.TICKET_DELIVERY_REUSED;
            findings.add(Finding.of(rule, reuse.otherShipmentId(), reuse.ticketNo()));
        }
        return findings;
    }

    /** K1/K2: brüt ve dara fişle 1 kg içinde tutmalı; öncelik net farkın bandından, en az Sarı. */
    private static @Nullable Finding compareEntry(
            String ruleCode, @Nullable ScaleWeights entered, @Nullable ScaleWeights read, ToleranceBands bands) {
        if (entered == null || read == null || sameTicket(entered, read)) {
            return null;
        }
        BigDecimal diffKg = entered.netKg().subtract(read.netKg());
        BigDecimal diffPct = Comparison.percentage(diffKg, entered.netKg());
        Severity severity = bands.classify(diffPct) == Band.RED ? Severity.RED : Severity.YELLOW;
        return new Finding(
                DiscrepancyType.DOCUMENT, severity, ruleCode, entered.toString(), read.toString(), diffKg, diffPct);
    }

    private static void addIfFound(List<Finding> findings, @Nullable Finding finding) {
        if (finding != null) {
            findings.add(finding);
        }
    }

    private static boolean sameTicket(ScaleWeights entered, ScaleWeights read) {
        return withinOneKg(entered.grossKg(), read.grossKg()) && withinOneKg(entered.tareKg(), read.tareKg());
    }

    private static boolean withinOneKg(BigDecimal left, BigDecimal right) {
        return left.subtract(right).abs().compareTo(SAME_TICKET_KG) <= 0;
    }

    /** K3: elle girişlerin miktar karşılaştırmasının fişlerle yapılan ikizi; aynı bantlar. */
    private static void compareTickets(ShipmentTickets tickets, ToleranceBands bands, List<Finding> findings) {
        ScaleWeights dispatch = tickets.dispatchWeights();
        ScaleWeights delivery = tickets.deliveryWeights();
        if (dispatch == null || delivery == null) {
            return;
        }
        BigDecimal diffKg = dispatch.netKg().subtract(delivery.netKg());
        BigDecimal diffPct = Comparison.percentage(diffKg, dispatch.netKg());
        Band band = bands.classify(diffPct);
        if (band != Band.GREEN) {
            findings.add(new Finding(
                    DiscrepancyType.QUANTITY,
                    band == Band.RED ? Severity.RED : Severity.YELLOW,
                    QUANTITY_TOLERANCE,
                    dispatch.netKg().toPlainString(),
                    delivery.netKg().toPlainString(),
                    diffKg,
                    diffPct));
        }
    }

    /** K4: boşluk, tire ve harf büyüklüğü yok sayılır; iki taraftan biri boşsa karşılaştırılmaz. */
    private static void comparePlate(
            ComparisonRule rule, @Nullable String plateNo, @Nullable TicketReading ticket, List<Finding> findings) {
        String expected = plate(plateNo);
        String actual = ticket == null ? null : plate(ticket.fields().plate());
        if (expected != null && actual != null && !Objects.equals(expected, actual)) {
            findings.add(Finding.of(
                    rule, plateNo, Objects.requireNonNull(ticket).fields().plate()));
        }
    }

    private static @Nullable String plate(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String compact = PLATE_NOISE.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
        return compact.isEmpty() ? null : compact;
    }
}
