package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.evidence.TicketRead;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code evidence.events.v1} dinleyicisi. Yalnızca kantar fişi okumalarını saklar; diğer kanıt olayları bu adımda
 * kullanılmaz ve onaylanır.
 */
@Component
final class EvidenceEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RecordTicketReadingUseCase recordTicketReadingUseCase;

    EvidenceEventsListener(RecordTicketReadingUseCase recordTicketReadingUseCase) {
        this.recordTicketReadingUseCase = recordTicketReadingUseCase;
    }

    @KafkaListener(topics = EvidenceEvents.TOPIC)
    void onEvidenceEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (!TicketRead.TYPE.equals(event.type())) {
            return;
        }
        TicketRead read = EVENT_READER.readValue(event.data(), TicketRead.class);
        recordTicketReadingUseCase.recordTicketReading(
                new RecordTicketReadingCommand(actorOf(event), TicketReadMapper.toReading(read)));
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
