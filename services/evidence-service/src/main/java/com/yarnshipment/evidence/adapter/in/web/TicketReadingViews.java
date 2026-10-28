package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceType;
import com.yarnshipment.evidence.adapter.in.web.api.model.ReadingProblem;
import com.yarnshipment.evidence.adapter.in.web.api.model.TicketReadingStatus;
import com.yarnshipment.evidence.adapter.in.web.api.model.TicketReadingView;
import com.yarnshipment.evidence.domain.ticket.ReadWeights;
import com.yarnshipment.evidence.domain.ticket.TicketFields;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import java.time.ZoneOffset;

/** Fiş okumasının API modeline dönüşümü. */
final class TicketReadingViews {

    private TicketReadingViews() {}

    static TicketReadingView toView(TicketReading reading) {
        TicketReadingView view = new TicketReadingView(
                reading.evidenceId(),
                reading.shipmentId(),
                EvidenceType.fromValue(reading.evidenceType().name()),
                TicketReadingStatus.fromValue(reading.status().name()),
                reading.problems().stream()
                        .map(problem -> ReadingProblem.fromValue(problem.name()))
                        .toList(),
                reading.readAt().atOffset(ZoneOffset.UTC));
        ReadWeights weights = reading.weights();
        if (weights != null) {
            view.setGrossKg(weights.grossKg());
            view.setTareKg(weights.tareKg());
            view.setNetKg(weights.netKg());
        }
        TicketFields fields = reading.fields();
        view.setPlate(fields.plate());
        view.setTicketNo(fields.ticketNo());
        view.setScaleName(fields.scaleName());
        if (fields.weighedAt() != null) {
            view.setWeighedAt(fields.weighedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }
}
