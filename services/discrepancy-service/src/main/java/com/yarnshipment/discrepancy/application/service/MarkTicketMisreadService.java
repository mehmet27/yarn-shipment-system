package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReviewPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReadingNotFoundException;
import java.time.Clock;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "AI yanıldı": inceleyen kişi fiş okumasının yanlış olduğunu işaretler. Uyuşmazlığın durumu değişmez; karar ayrıca
 * verilir. Okuma uyuşmazlığın sevkiyatına ait olmalıdır; aynı okuma ikinci kez işaretlenmez.
 */
@Service
public class MarkTicketMisreadService implements MarkTicketMisreadUseCase {

    private static final String ACTION = "DISCREPANCY_RESOLVE";

    private final DiscrepancyAccess access;
    private final TicketReviewPort reviewPort;
    private final TicketReviews reviews;
    private final Clock clock;

    public MarkTicketMisreadService(
            DiscrepancyQueryPort queryPort, TicketReviewPort reviewPort, ScopeGuard scopeGuard, Clock clock) {
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
        this.reviewPort = reviewPort;
        this.reviews = new TicketReviews(reviewPort);
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<ReviewedTicket> markMisread(MarkTicketMisreadCommand command) {
        Discrepancy discrepancy = access.load(command.actor(), command.discrepancyId(), ACTION);
        List<ReviewedTicket> tickets = reviews.of(discrepancy.shipmentId());
        ReviewedTicket ticket = tickets.stream()
                .filter(found -> found.reading().evidenceId().equals(command.evidenceId()))
                .findFirst()
                .orElseThrow(() -> new TicketReadingNotFoundException(command.evidenceId()));
        if (ticket.misread() != null) {
            return tickets;
        }
        reviewPort.saveMisread(new TicketMisread(
                command.evidenceId(),
                discrepancy.id(),
                command.actor().userId(),
                clock.instant(),
                noteOf(command.note())));
        return reviews.of(discrepancy.shipmentId());
    }

    private static @Nullable String noteOf(@Nullable String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }
}
