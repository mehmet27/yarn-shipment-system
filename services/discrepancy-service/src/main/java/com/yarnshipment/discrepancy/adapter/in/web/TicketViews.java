package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyTicketView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.TicketMisreadView;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Fiş panelinin API modeline dönüşümü; kilolar ondalık dizedir (API-03). */
final class TicketViews {

    private TicketViews() {}

    static List<DiscrepancyTicketView> toViews(List<ReviewedTicket> tickets, Actor actor) {
        boolean reviewer = actor.hasPermission(Permission.DISCREPANCY_RESOLVE);
        return tickets.stream().map(ticket -> toView(ticket, reviewer)).toList();
    }

    private static DiscrepancyTicketView toView(ReviewedTicket ticket, boolean reviewer) {
        TicketReading reading = ticket.reading();
        TicketMisread misread = ticket.misread();
        List<DiscrepancyTicketView.AllowedActionsEnum> actions = reviewer && misread == null
                ? List.of(DiscrepancyTicketView.AllowedActionsEnum.MARK_MISREAD)
                : List.of();
        DiscrepancyTicketView view = new DiscrepancyTicketView(
                reading.evidenceId(),
                DiscrepancyTicketView.SideEnum.fromValue(reading.side().name()),
                reading.read() ? DiscrepancyTicketView.StatusEnum.READ : DiscrepancyTicketView.StatusEnum.UNREADABLE,
                reading.problems(),
                utc(reading.readAt()),
                actions);
        ScaleWeights weights = reading.weights();
        if (weights != null) {
            view.grossKg(weights.grossKg().toPlainString())
                    .tareKg(weights.tareKg().toPlainString())
                    .netKg(weights.netKg().toPlainString());
        }
        TicketFields fields = reading.fields();
        view.enteredNetKg(plain(ticket.enteredNetKg()))
                .plate(fields.plate())
                .ticketNo(fields.ticketNo())
                .scaleName(fields.scaleName())
                .weighedAt(fields.weighedAt() == null ? null : utc(fields.weighedAt()));
        if (misread != null) {
            view.misread(new TicketMisreadView(misread.markedBy(), utc(misread.markedAt())).note(misread.note()));
        }
        return view;
    }

    private static @Nullable String plain(@Nullable BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
