package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentCommand;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.events.evidence.TicketRead;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code evidence.events.v1} dinleyicisi. Kantar fişi okumalarını saklar; imzalı teslim belgesi yüklenince sevkiyatın
 * açık "belge yok" uyuşmazlığını kapatır. Diğer kanıt olayları kullanılmaz ve onaylanır.
 */
@Component
final class EvidenceEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** evidence-service'teki imzalı teslim belgesi (irsaliye) kanıt türü. */
    private static final String SIGNED_DELIVERY_DOC = "SIGNED_DELIVERY_DOC";

    private final RecordTicketReadingUseCase recordTicketReadingUseCase;
    private final CloseMissingDocumentUseCase closeMissingDocumentUseCase;

    EvidenceEventsListener(
            RecordTicketReadingUseCase recordTicketReadingUseCase,
            CloseMissingDocumentUseCase closeMissingDocumentUseCase) {
        this.recordTicketReadingUseCase = recordTicketReadingUseCase;
        this.closeMissingDocumentUseCase = closeMissingDocumentUseCase;
    }

    @KafkaListener(topics = EvidenceEvents.TOPIC)
    void onEvidenceEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (EvidenceUploaded.TYPE.equals(event.type())) {
            onEvidenceUploaded(EVENT_READER.readValue(event.data(), EvidenceUploaded.class));
        } else if (TicketRead.TYPE.equals(event.type())) {
            TicketRead read = EVENT_READER.readValue(event.data(), TicketRead.class);
            recordTicketReadingUseCase.recordTicketReading(
                    new RecordTicketReadingCommand(actorOf(event), TicketReadMapper.toReading(read)));
        }
    }

    private void onEvidenceUploaded(EvidenceUploaded uploaded) {
        if (SIGNED_DELIVERY_DOC.equals(uploaded.evidenceType())) {
            closeMissingDocumentUseCase.closeMissingDocument(
                    new CloseMissingDocumentCommand(uploaded.shipmentId(), uploaded.evidenceId()));
        }
    }

    /** Okumayı fotoğrafı yükleyen kişi ister; olay her zaman onun kimliğini taşır (TicketReadingAdapter). */
    private static UUID actorOf(IncomingEvent event) {
        String actorId = event.actorId();
        if (actorId == null) {
            throw new IllegalArgumentException("TicketRead without actorId eventId=" + event.id());
        }
        return UUID.fromString(actorId);
    }
}
