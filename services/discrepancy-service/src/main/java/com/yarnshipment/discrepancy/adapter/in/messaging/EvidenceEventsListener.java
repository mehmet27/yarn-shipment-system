package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceCommand;
import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordEvidenceUploadUseCase;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketUseCase;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.evidence.EvidenceReuseDetected;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.events.evidence.TicketRead;
import com.yarnshipment.events.evidence.TicketUnverified;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code evidence.events.v1} dinleyicisi. Kantar fişi okumalarını saklar; yüklenen kanıtın türünü sevkiyata yazar ve
 * sonradan gelen belge ya da fotoğrafın "eksik" uyuşmazlığını kapatır; tekrar kullanılan fotoğraf için uyuşmazlık
 * açar; doğrulanamayan kantar fişi için de uyuşmazlık açar. Diğer kanıt olayları kullanılmaz ve onaylanır.
 */
@Component
final class EvidenceEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RecordTicketReadingUseCase recordTicketReadingUseCase;
    private final RecordEvidenceUploadUseCase recordEvidenceUploadUseCase;
    private final OpenReusedEvidenceUseCase openReusedEvidenceUseCase;
    private final OpenUnverifiedTicketUseCase openUnverifiedTicketUseCase;

    EvidenceEventsListener(
            RecordTicketReadingUseCase recordTicketReadingUseCase,
            RecordEvidenceUploadUseCase recordEvidenceUploadUseCase,
            OpenReusedEvidenceUseCase openReusedEvidenceUseCase,
            OpenUnverifiedTicketUseCase openUnverifiedTicketUseCase) {
        this.recordTicketReadingUseCase = recordTicketReadingUseCase;
        this.recordEvidenceUploadUseCase = recordEvidenceUploadUseCase;
        this.openReusedEvidenceUseCase = openReusedEvidenceUseCase;
        this.openUnverifiedTicketUseCase = openUnverifiedTicketUseCase;
    }

    @KafkaListener(topics = EvidenceEvents.TOPIC)
    void onEvidenceEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (EvidenceUploaded.TYPE.equals(event.type())) {
            EvidenceUploaded uploaded = EVENT_READER.readValue(event.data(), EvidenceUploaded.class);
            recordEvidenceUploadUseCase.recordEvidenceUpload(new RecordEvidenceUploadCommand(
                    uploaded.shipmentId(), uploaded.evidenceId(), uploaded.evidenceType(), uploaded.uploadedAt()));
        } else if (EvidenceReuseDetected.TYPE.equals(event.type())) {
            EvidenceReuseDetected reuse = EVENT_READER.readValue(event.data(), EvidenceReuseDetected.class);
            openReusedEvidenceUseCase.openReusedEvidence(new OpenReusedEvidenceCommand(
                    event.id(), event.type(), reuse.evidenceId(), reuse.shipmentId(), reuse.duplicateOfId()));
        } else if (TicketRead.TYPE.equals(event.type())) {
            TicketRead read = EVENT_READER.readValue(event.data(), TicketRead.class);
            recordTicketReadingUseCase.recordTicketReading(
                    new RecordTicketReadingCommand(actorOf(event), TicketReadMapper.toReading(read)));
        } else if (TicketUnverified.TYPE.equals(event.type())) {
            TicketUnverified unverified = EVENT_READER.readValue(event.data(), TicketUnverified.class);
            openUnverifiedTicketUseCase.openUnverifiedTicket(new OpenUnverifiedTicketCommand(
                    event.id(),
                    event.type(),
                    unverified.evidenceId(),
                    unverified.shipmentId(),
                    TicketSide.valueOf(unverified.evidenceType()),
                    unverified.reason()));
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
