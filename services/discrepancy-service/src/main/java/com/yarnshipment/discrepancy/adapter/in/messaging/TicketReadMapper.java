package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.events.evidence.TicketRead;

/** Fiş okundu olayını okuma kaydına çevirir. */
final class TicketReadMapper {

    private static final String READ = "READ";

    private TicketReadMapper() {}

    /** Durumu kilolarla çelişen olay işlenemez; kalıcı hatadır, DLT'ye gider (EVT-09). */
    static TicketReading toReading(TicketRead event) {
        TicketRead.Weights weights = event.weights();
        if (READ.equals(event.status()) != (weights != null)) {
            throw new IllegalArgumentException(
                    "TicketRead status and weights disagree evidenceId=" + event.evidenceId());
        }
        TicketRead.Fields fields = event.fields();
        return new TicketReading(
                event.evidenceId(),
                event.shipmentId(),
                TicketSide.valueOf(event.evidenceType()),
                weights == null ? null : new ScaleWeights(weights.grossKg(), weights.tareKg(), weights.netKg()),
                new TicketFields(fields.plate(), fields.weighedAt(), fields.ticketNo(), fields.scaleName()),
                event.problems(),
                event.readAt());
    }
}
