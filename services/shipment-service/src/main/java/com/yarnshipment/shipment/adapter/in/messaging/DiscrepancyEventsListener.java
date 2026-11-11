package com.yarnshipment.shipment.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentUseCase;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code discrepancy.events.v1} dinleyicisi. Haklı bulunan kalite itirazının kilosunu bakiye defterine yansıtır;
 * diğer olaylar kullanılmaz ve onaylanır.
 */
@Component
final class DiscrepancyEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Olayda kullanıcı yoksa deftere sistem kullanıcısı yazılır. */
    private static final UUID SYSTEM_USER = new UUID(0L, 0L);

    private final RecordClaimAdjustmentUseCase claimAdjustment;

    DiscrepancyEventsListener(RecordClaimAdjustmentUseCase claimAdjustment) {
        this.claimAdjustment = claimAdjustment;
    }

    @KafkaListener(topics = DiscrepancyEvents.TOPIC)
    void onDiscrepancyEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (!DiscrepancyResolved.TYPE.equals(event.type())) {
            return;
        }
        DiscrepancyResolved resolved = EVENT_READER.readValue(event.data(), DiscrepancyResolved.class);
        if (resolved.claimedKg() == null || resolved.claimedKg().signum() <= 0) {
            return;
        }
        claimAdjustment.recordClaimAdjustment(new RecordClaimAdjustmentCommand(
                resolved.discrepancyId(),
                resolved.shipmentId(),
                resolved.claimedKg(),
                resolved.resolvedAt(),
                event.actorId() == null ? SYSTEM_USER : UUID.fromString(event.actorId())));
    }
}
