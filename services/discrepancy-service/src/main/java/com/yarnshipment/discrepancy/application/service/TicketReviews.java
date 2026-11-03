package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.out.TicketReviewPort;
import com.yarnshipment.discrepancy.domain.ticket.EnteredNets;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fiş panelini kurar: her okumanın yanına aynı tarafın elle girilen neti ve varsa işareti konur. */
final class TicketReviews {

    private final TicketReviewPort reviewPort;

    TicketReviews(TicketReviewPort reviewPort) {
        this.reviewPort = reviewPort;
    }

    List<ReviewedTicket> of(UUID shipmentId) {
        Optional<EnteredNets> entered = reviewPort.findEnteredNets(shipmentId);
        Map<UUID, TicketMisread> misreads = reviewPort.findMisreads(shipmentId).stream()
                .collect(Collectors.toMap(TicketMisread::evidenceId, Function.identity()));
        return reviewPort.listReadings(shipmentId).stream()
                .map(reading -> new ReviewedTicket(
                        reading,
                        entered.map(nets -> nets.of(reading.side())).orElse(null),
                        misreads.get(reading.evidenceId())))
                .toList();
    }
}
