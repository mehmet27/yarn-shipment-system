package com.yarnshipment.discrepancy.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceCommand;
import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketUseCase;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.evidence.EvidenceReuseDetected;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.events.evidence.TicketRead;
import com.yarnshipment.events.evidence.TicketUnverified;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EvidenceEventsListenerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e2");
    private static final String ACTOR = "0192f0a0-0000-7000-8000-00000000d002";
    private static final String EVIDENCE = "0192f0a0-0000-7000-8000-00000000f002";
    private static final Instant AT = Instant.parse("2026-10-23T08:00:00Z");
    /** Sözleşmede olmayan {@code futureField} yok sayılır (EVT-06). */
    private static final String READ = """
            {"evidenceId":"%s","shipmentId":"%s","evidenceType":"DISPATCH_SCALE","status":"READ",
             "weights":{"grossKg":12480,"tareKg":8210,"netKg":4270},
             "fields":{"plate":"34 ABC 123","weighedAt":"2026-10-23T06:42:00Z","ticketNo":"004512",
                       "scaleName":null},
             "problems":["DATE_OUT_OF_RANGE"],"readAt":"2026-10-23T08:00:00Z","futureField":true}
            """.formatted(EVIDENCE, TestComparisons.SHIPMENT);

    private static final String UNREADABLE = """
            {"evidenceId":"%s","shipmentId":"%s","evidenceType":"DELIVERY_SCALE","status":"UNREADABLE",
             "fields":{},"problems":["WEIGHT_NOT_READ","BLURRY"],"readAt":"2026-10-23T08:00:00Z"}
            """.formatted(EVIDENCE, TestComparisons.SHIPMENT);

    private final RecordTicketReadingUseCase useCase = mock(RecordTicketReadingUseCase.class);
    private final RecordEvidenceUploadUseCase recordUpload = mock(RecordEvidenceUploadUseCase.class);
    private final OpenReusedEvidenceUseCase openReused = mock(OpenReusedEvidenceUseCase.class);
    private final OpenUnverifiedTicketUseCase openUnverified = mock(OpenUnverifiedTicketUseCase.class);
    private final EvidenceEventsListener listener =
            new EvidenceEventsListener(useCase, recordUpload, openReused, openUnverified);

    @Test
    @DisplayName("Fiş okundu olayı kilolar, alanlar ve yükleyen kişiyle okuma kaydına çevrilir")
    void shouldRecordReadTicket() {
        listener.onEvidenceEvent(record(TicketRead.TYPE, READ, Map.of("actorId", ACTOR)));

        RecordTicketReadingCommand command = captured();
        TicketReading reading = command.reading();
        ScaleWeights weights = Objects.requireNonNull(reading.weights());
        assertThat(command.actorId()).isEqualTo(UUID.fromString(ACTOR));
        assertThat(reading.evidenceId()).isEqualTo(UUID.fromString(EVIDENCE));
        assertThat(reading.side()).isEqualTo(TicketSide.DISPATCH_SCALE);
        assertThat(weights.netKg()).isEqualByComparingTo("4270");
        assertThat(reading.fields().plate()).isEqualTo("34 ABC 123");
        assertThat(reading.fields().scaleName()).isNull();
        assertThat(reading.problems()).containsExactly("DATE_OUT_OF_RANGE");
        assertThat(reading.readAt()).isEqualTo(AT);
    }

    @Test
    @DisplayName("Okunamayan fiş kilosuz ve sorunlarıyla kaydedilir")
    void shouldRecordUnreadableTicket() {
        listener.onEvidenceEvent(record(TicketRead.TYPE, UNREADABLE, Map.of("actorId", ACTOR)));

        TicketReading reading = captured().reading();
        assertThat(reading.read()).isFalse();
        assertThat(reading.side()).isEqualTo(TicketSide.DELIVERY_SCALE);
        assertThat(reading.problems()).containsExactly("WEIGHT_NOT_READ", "BLURRY");
    }

    @Test
    @DisplayName("Her kanıt yüklemesi türüyle sevkiyata yazılır; eksik belge ya da fotoğraf uyuşmazlığı kapanabilir")
    void shouldRecordEveryUpload() {
        String uploaded = """
                {"evidenceId":"%s","shipmentId":"%s","evidenceType":"%s","sha256":"abc",
                 "uploadedAt":"2026-10-23T08:00:00Z"}
                """.formatted(EVIDENCE, TestComparisons.SHIPMENT, "SIGNED_DELIVERY_DOC");

        listener.onEvidenceEvent(record(EvidenceUploaded.TYPE, uploaded, Map.of("actorId", ACTOR)));

        verify(recordUpload)
                .recordEvidenceUpload(new RecordEvidenceUploadCommand(
                        TestComparisons.SHIPMENT, UUID.fromString(EVIDENCE), "SIGNED_DELIVERY_DOC", AT));
        verifyNoInteractions(useCase, openReused);
    }

    @Test
    @DisplayName("Tekrar kullanılan kanıt olayı, olay kimliğiyle fotoğraf uyuşmazlığı komutuna çevrilir")
    void shouldOpenReusedEvidence() {
        String original = "0192f0a0-0000-7000-8000-00000000f001";
        String reuse = """
                {"evidenceId":"%s","shipmentId":"%s","duplicateOfId":"%s","originalShipmentId":"%s",
                 "detectedAt":"2026-10-23T08:00:00Z"}
                """.formatted(EVIDENCE, TestComparisons.SHIPMENT, original, UUID.randomUUID());

        listener.onEvidenceEvent(record(EvidenceReuseDetected.TYPE, reuse, Map.of()));

        verify(openReused)
                .openReusedEvidence(new OpenReusedEvidenceCommand(
                        EVENT,
                        EvidenceReuseDetected.TYPE,
                        UUID.fromString(EVIDENCE),
                        TestComparisons.SHIPMENT,
                        UUID.fromString(original)));
        verifyNoInteractions(useCase, recordUpload);
    }

    @Test
    @DisplayName("Fiş doğrulanamadı olayı, olay kimliği, taraf ve gerekçeyle uyuşmazlık komutuna çevrilir")
    void shouldOpenUnverifiedTicket() {
        String unverified = """
                {"evidenceId":"%s","shipmentId":"%s","evidenceType":"DELIVERY_SCALE","reason":"TICKET_DAMAGED",
                 "declaredAt":"2026-10-23T08:00:00Z"}
                """.formatted(EVIDENCE, TestComparisons.SHIPMENT);

        listener.onEvidenceEvent(record(TicketUnverified.TYPE, unverified, Map.of("actorId", ACTOR)));

        verify(openUnverified)
                .openUnverifiedTicket(new OpenUnverifiedTicketCommand(
                        EVENT,
                        TicketUnverified.TYPE,
                        UUID.fromString(EVIDENCE),
                        TestComparisons.SHIPMENT,
                        TicketSide.DELIVERY_SCALE,
                        "TICKET_DAMAGED"));
        verifyNoInteractions(useCase, recordUpload, openReused);
    }

    @Test
    @DisplayName("Bilinmeyen kanıt olayları kullanılmaz")
    void shouldIgnoreOtherEvents() {
        listener.onEvidenceEvent(record("com.yarnshipment.evidence.evidence-invalidated.v1", "{}", Map.of()));

        verifyNoInteractions(useCase, recordUpload, openReused, openUnverified);
    }

    @Test
    @DisplayName("Yapan kişiyi taşımayan ya da durumu kilolarla çelişen olay kalıcı hatadır")
    void shouldRejectInconsistentEvent() {
        String readWithoutWeights = READ.replaceFirst("\"weights\":\\{[^}]*},", "");

        assertThatThrownBy(() -> listener.onEvidenceEvent(record(TicketRead.TYPE, READ, Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        listener.onEvidenceEvent(record(TicketRead.TYPE, readWithoutWeights, Map.of("actorId", ACTOR))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(useCase, recordUpload, openReused);
    }

    private RecordTicketReadingCommand captured() {
        ArgumentCaptor<RecordTicketReadingCommand> command = ArgumentCaptor.forClass(RecordTicketReadingCommand.class);
        verify(useCase).recordTicketReading(command.capture());
        return command.getValue();
    }

    private static ConsumerRecord<String, String> record(String type, String payload, Map<String, String> headers) {
        OutboxEvent event = new OutboxEvent(EVENT, AT, TestComparisons.SHIPMENT, type, payload, headers, AT);
        ProducerRecord<String, String> produced =
                CloudEventHeaders.toRecord(EvidenceEvents.TOPIC, "/yarn-shipment/evidence-service", event);
        ConsumerRecord<String, String> consumed =
                new ConsumerRecord<>(EvidenceEvents.TOPIC, 0, 0L, produced.key(), produced.value());
        for (Header header : produced.headers()) {
            consumed.headers().add(header);
        }
        return consumed;
    }
}
