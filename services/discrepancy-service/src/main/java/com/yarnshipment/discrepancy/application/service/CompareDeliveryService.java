package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryCommand;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryUseCase;
import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.BusinessDays;
import com.yarnshipment.discrepancy.domain.model.Comparison;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.TicketChecks;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teslim girişini karşılaştırır. Aynı olay ya da aynı sevkiyat için ikinci teslim tekrar karşılaştırılmaz: tutanak
 * gönderildiği an kilitlenir, değişiklik düzeltme talebiyle gelir. O ana kadar okunmuş kantar fişleri de elle
 * girişlerle ve birbiriyle karşılaştırılır (K1–K5).
 */
@Service
public class CompareDeliveryService implements CompareDeliveryUseCase {

    /** İş günü hesabı fabrikanın saat dilimiyle yapılır. */
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    private final ProcessedEventPort processedEvents;
    private final ToleranceRulePort toleranceRules;
    private final ComparisonPort comparisons;
    private final TicketReadingPort ticketReadings;
    private final Clock clock;

    public CompareDeliveryService(
            ProcessedEventPort processedEvents,
            ToleranceRulePort toleranceRules,
            ComparisonPort comparisons,
            TicketReadingPort ticketReadings,
            Clock clock) {
        this.processedEvents = processedEvents;
        this.toleranceRules = toleranceRules;
        this.comparisons = comparisons;
        this.ticketReadings = ticketReadings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void compareDelivery(CompareDeliveryCommand command) {
        ComparisonInput input = command.input();
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())
                || comparisons.hasComparison(input.shipmentId())) {
            return;
        }
        Instant now = clock.instant();
        ToleranceBands bands = toleranceRules.findGeneralRule(now).orElse(ToleranceBands.REPORT_DEFAULT);
        ShipmentTickets tickets = ticketReadings.findTickets(input.shipmentId());
        comparisons.saveComparison(new CompletedComparison(
                input,
                Comparison.compare(input, bands).plus(TicketChecks.compare(command.entries(), tickets, bands)),
                now,
                BusinessDays.plus(now, BusinessDays.DISCREPANCY_DUE_DAYS, FACTORY_ZONE),
                command.actorId()));
    }
}
