package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadCommand;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReviewPort;
import com.yarnshipment.discrepancy.domain.ticket.EnteredNets;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketReadingNotFoundException;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Uyuşmazlık ekranının fiş paneli ve "AI yanıldı" işareti. */
class DiscrepancyTicketServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final BigDecimal DISPATCH_NET = TestComparisons.DISPATCH_KG;
    private static final BigDecimal DELIVERY_NET = BigDecimal.valueOf(1990);

    private final DiscrepancyQueryPort queries = mock(DiscrepancyQueryPort.class);
    private final TicketReviewPort reviewPort = mock(TicketReviewPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);
    private final GetDiscrepancyTicketsService get = new GetDiscrepancyTicketsService(queries, reviewPort, scopeGuard);
    private final MarkTicketMisreadService mark = new MarkTicketMisreadService(queries, reviewPort, scopeGuard, CLOCK);
    private final TicketReading dispatch = TestTickets.reading(TicketSide.DISPATCH_SCALE, TestTickets.weights());
    private final TicketReading delivery = TestTickets.reading(TicketSide.DELIVERY_SCALE, null);
    private final Actor quality = TestTokens.mfaActor("quality");

    @BeforeEach
    void givenTickets() {
        when(queries.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));
        when(reviewPort.listReadings(TestDiscrepancies.SHIPMENT)).thenReturn(List.of(delivery, dispatch));
        when(reviewPort.findEnteredNets(TestDiscrepancies.SHIPMENT))
                .thenReturn(Optional.of(new EnteredNets(DISPATCH_NET, DELIVERY_NET)));
    }

    @Test
    @DisplayName("Her okumanın yanına aynı tarafın elle girilen neti ve varsa işareti konur")
    void shouldPairReadingsWithEnteredNets() {
        TicketMisread misread =
                new TicketMisread(dispatch.evidenceId(), TestDiscrepancies.ID, UUID.randomUUID(), NOW, null);
        when(reviewPort.findMisreads(TestDiscrepancies.SHIPMENT)).thenReturn(List.of(misread));

        List<ReviewedTicket> tickets = get.getTickets(new GetDiscrepancyQuery(quality, TestDiscrepancies.ID));

        assertThat(tickets).extracting(ReviewedTicket::reading).containsExactly(delivery, dispatch);
        assertThat(tickets).extracting(ReviewedTicket::enteredNetKg).containsExactly(DELIVERY_NET, DISPATCH_NET);
        assertThat(tickets).extracting(ReviewedTicket::misread).containsExactly(null, misread);
    }

    @Test
    @DisplayName("İşaret işaretleyenin adıyla ve kırpılmış notla yazılır; işaretli okuma tekrar yazılmaz")
    void shouldMarkMisreadOnce() {
        mark.markMisread(
                new MarkTicketMisreadCommand(quality, TestDiscrepancies.ID, dispatch.evidenceId(), "  brüt  "));

        ArgumentCaptor<TicketMisread> saved = ArgumentCaptor.forClass(TicketMisread.class);
        verify(reviewPort).saveMisread(saved.capture());
        assertThat(saved.getValue().markedBy()).isEqualTo(quality.userId());
        assertThat(saved.getValue().markedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().note()).isEqualTo("brüt");
        assertThat(saved.getValue().discrepancyId()).isEqualTo(TestDiscrepancies.ID);

        when(reviewPort.findMisreads(TestDiscrepancies.SHIPMENT)).thenReturn(List.of(saved.getValue()));
        mark.markMisread(new MarkTicketMisreadCommand(quality, TestDiscrepancies.ID, dispatch.evidenceId(), null));
        verify(reviewPort).saveMisread(any());
    }

    @Test
    @DisplayName("Başka sevkiyatın okuması işaretlenemez")
    void shouldRejectForeignReading() {
        MarkTicketMisreadCommand command =
                new MarkTicketMisreadCommand(quality, TestDiscrepancies.ID, UUID.randomUUID(), null);

        assertThatThrownBy(() -> mark.markMisread(command)).isInstanceOf(TicketReadingNotFoundException.class);
        verify(reviewPort, never()).saveMisread(any());
    }
}
