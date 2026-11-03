package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.TestTickets;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedCommand;
import com.yarnshipment.evidence.application.port.out.TicketReadingStorePort;
import com.yarnshipment.evidence.application.port.out.TicketUnverifiedPort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketNotUnreadableException;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclareTicketUnverifiedServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final UploadIntentPort intents = mock(UploadIntentPort.class);
    private final TicketReadingStorePort readings = mock(TicketReadingStorePort.class);
    private final TicketUnverifiedPort unverifiedPort = mock(TicketUnverifiedPort.class);
    private final DeclareTicketUnverifiedService service = new DeclareTicketUnverifiedService(
            intents, readings, unverifiedPort, new ScopeGuard(event -> {}, CLOCK), CLOCK);
    private final Actor warehouse = TestTokens.actor("warehouse");
    private final DeclareTicketUnverifiedCommand command =
            new DeclareTicketUnverifiedCommand(warehouse, TestUploads.EVIDENCE_ID, UnverifiedReason.NO_TICKET);

    @Test
    @DisplayName("Okunamayan fiş için gerekçe yükleyenin adıyla ve okuma zamanıyla kaydedilir")
    void shouldDeclareUnreadableTicket() {
        givenIntent(warehouse.userId());
        TicketReading unreadable = reading(TicketScan.unreadableImage(ReadingProblem.BLURRY));
        when(readings.findReading(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(unreadable));

        TicketUnverified unverified = service.declareUnverified(command);

        assertThat(unverified.reason()).isEqualTo(UnverifiedReason.NO_TICKET);
        assertThat(unverified.evidenceType()).isEqualTo(EvidenceType.DISPATCH_SCALE);
        assertThat(unverified.declaredAt()).isEqualTo(TestUploads.NOW);
        verify(unverifiedPort).saveUnverified(unverified);
    }

    @Test
    @DisplayName("Tekrar istekte ilk gerekçe döner, yeni kayıt yazılmaz")
    void shouldReturnExisting() {
        givenIntent(warehouse.userId());
        TicketUnverified existing = new TicketUnverified(
                TestUploads.EVIDENCE_ID,
                UUID.randomUUID(),
                EvidenceType.DISPATCH_SCALE,
                UnverifiedReason.OTHER,
                TestUploads.NOW);
        when(unverifiedPort.findUnverified(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(existing));

        assertThat(service.declareUnverified(command)).isEqualTo(existing);
        verify(unverifiedPort, never()).saveUnverified(any());
    }

    @Test
    @DisplayName("Başkasının kanıtı, okunmamış ya da güvenle okunmuş fiş için gerekçe yazılamaz")
    void shouldRejectInvalidRequests() {
        givenIntent(UUID.randomUUID());
        assertThatThrownBy(() -> service.declareUnverified(command)).isInstanceOf(UploadIntentNotFoundException.class);

        givenIntent(warehouse.userId());
        assertThatThrownBy(() -> service.declareUnverified(command)).isInstanceOf(TicketNotUnreadableException.class);

        when(readings.findReading(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(reading(TestTickets.clearScan())));
        assertThatThrownBy(() -> service.declareUnverified(command)).isInstanceOf(TicketNotUnreadableException.class);
        verify(unverifiedPort, never()).saveUnverified(any());
    }

    private void givenIntent(UUID requestedBy) {
        when(intents.findIntent(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(TestUploads.intent(requestedBy)));
    }

    private TicketReading reading(TicketScan scan) {
        return TicketReading.evaluate(
                TestTickets.file(warehouse.userId(), EvidenceType.DISPATCH_SCALE),
                scan,
                TestTickets.VERSION,
                TestUploads.NOW);
    }
}
