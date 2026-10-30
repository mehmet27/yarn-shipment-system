package com.yarnshipment.discrepancy.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentCommand;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.events.evidence.TicketRead;
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

    private static final String UPLOADED = """
            {"evidenceId":"%s","shipmentId":"%s","evidenceType":"%s","sha256":"abc",
             "uploadedAt":"2026-10-23T08:00:00Z"}
            """;

    private final RecordTicketReadingUseCase useCase = mock(RecordTicketReadingUseCase.class);
    private final CloseMissingDocumentUseCase closeMissingDocument = mock(CloseMissingDocumentUseCase.class);
    private final EvidenceEventsListener listener = new EvidenceEventsListener(useCase, closeMissingDocument);

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
    @DisplayName("İmzalı teslim belgesi yüklenince sevkiyatın belge yok uyuşmazlığı kapatılır")
    void shouldCloseMissingDocumentOnSignedDocumentUpload() {
        String uploaded = UPLOADED.formatted(EVIDENCE, TestComparisons.SHIPMENT, "SIGNED_DELIVERY_DOC");

        listener.onEvidenceEvent(record(EvidenceUploaded.TYPE, uploaded, Map.of("actorId", ACTOR)));

        verify(closeMissingDocument)
                .closeMissingDocument(
                        new CloseMissingDocumentCommand(TestComparisons.SHIPMENT, UUID.fromString(EVIDENCE)));
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Diğer kanıt yüklemeleri ve olaylar saklanmaz, uyuşmazlık kapatmaz")
    void shouldIgnoreOtherEvents() {
        String scale = UPLOADED.formatted(EVIDENCE, TestComparisons.SHIPMENT, "DELIVERY_SCALE");

        listener.onEvidenceEvent(record(EvidenceUploaded.TYPE, scale, Map.of()));
        listener.onEvidenceEvent(record("com.yarnshipment.evidence.evidence-reuse-detected.v1", "{}", Map.of()));

        verifyNoInteractions(useCase, closeMissingDocument);
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
        verifyNoInteractions(useCase, closeMissingDocument);
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
