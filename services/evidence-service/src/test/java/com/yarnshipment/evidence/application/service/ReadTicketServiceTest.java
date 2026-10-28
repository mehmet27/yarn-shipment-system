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
import com.yarnshipment.evidence.application.port.in.ReadTicketCommand;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.TicketReaderPort;
import com.yarnshipment.evidence.application.port.out.TicketReadingStorePort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketReadingNotSupportedException;
import com.yarnshipment.evidence.domain.ticket.TicketReadingStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReadTicketServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);
    private static final byte[] IMAGE = {1, 2, 3};

    private final UploadIntentPort intents = mock(UploadIntentPort.class);
    private final EvidenceStorePort evidence = mock(EvidenceStorePort.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final TicketReaderPort reader = mock(TicketReaderPort.class);
    private final TicketReadingStorePort readings = mock(TicketReadingStorePort.class);
    private final ReadTicketService service = new ReadTicketService(
            intents, evidence, storage, reader, readings, new ScopeGuard(event -> {}, CLOCK), CLOCK);
    private final Actor warehouse = TestTokens.actor("warehouse");
    private final ReadTicketCommand command = new ReadTicketCommand(warehouse, TestUploads.EVIDENCE_ID);

    @Test
    @DisplayName("Yükleyen kişi fişi okutur; sonuç kurallardan geçip kaydedilir")
    void shouldReadAndSave() {
        EvidenceFile file = givenUploaded(warehouse.userId());
        when(storage.download(file.storageKey())).thenReturn(Optional.of(IMAGE));
        when(reader.scanTicket(IMAGE, TestUploads.JPEG)).thenReturn(TestTickets.clearScan());
        when(reader.readerVersion()).thenReturn(TestTickets.VERSION);

        TicketReading reading = service.readTicket(command);

        assertThat(reading.status()).isEqualTo(TicketReadingStatus.READ);
        assertThat(reading.readAt()).isEqualTo(TestUploads.NOW);
        verify(readings).saveReading(reading);
    }

    @Test
    @DisplayName("Aynı kanıt tekrar okutulursa kayıtlı okuma döner, model çağrılmaz")
    void shouldReturnExistingReading() {
        givenUploaded(warehouse.userId());
        TicketReading existing = TicketReading.evaluate(
                TestTickets.file(warehouse.userId(), EvidenceType.DISPATCH_SCALE),
                TestTickets.clearScan(),
                TestTickets.VERSION,
                TestUploads.NOW);
        when(readings.findReading(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(existing));

        assertThat(service.readTicket(command)).isEqualTo(existing);
        verify(reader, never()).scanTicket(any(), any());
    }

    @Test
    @DisplayName("Başkasının kanıtı okunamaz; onaylanmamış ya da depoda olmayan dosya okunamaz")
    void shouldRejectInvalidRequests() {
        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(UUID.randomUUID())));
        assertThatThrownBy(() -> service.readTicket(command)).isInstanceOf(UploadIntentNotFoundException.class);

        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(warehouse.userId())));
        when(readings.findReading(TestUploads.EVIDENCE_ID)).thenReturn(Optional.empty());
        when(evidence.findEvidence(TestUploads.EVIDENCE_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.readTicket(command)).isInstanceOf(UploadNotCompletedException.class);

        givenUploaded(warehouse.userId());
        when(storage.download(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.readTicket(command)).isInstanceOf(UploadNotCompletedException.class);
        verify(readings, never()).saveReading(any());
    }

    @Test
    @DisplayName("Kantar fişi olmayan kanıt okunmaz")
    void shouldRejectOtherEvidenceTypes() {
        UploadIntent base = TestUploads.intent(warehouse.userId());
        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(new UploadIntent(
                        base.id(),
                        base.shipmentId(),
                        base.siteId(),
                        TestUploads.declared(EvidenceType.BAG_LABEL),
                        base.storageKey(),
                        base.requestedBy(),
                        base.requestedAt(),
                        base.expiresAt())));

        assertThatThrownBy(() -> service.readTicket(command)).isInstanceOf(TicketReadingNotSupportedException.class);
    }

    private EvidenceFile givenUploaded(UUID user) {
        EvidenceFile file = TestTickets.file(user, EvidenceType.DISPATCH_SCALE);
        when(intents.findIntent(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(TestUploads.intent(user)));
        when(readings.findReading(TestUploads.EVIDENCE_ID)).thenReturn(Optional.empty());
        when(evidence.findEvidence(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(file));
        return file;
    }
}
