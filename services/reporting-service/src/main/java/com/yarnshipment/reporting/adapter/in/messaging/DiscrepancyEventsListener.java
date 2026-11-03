package com.yarnshipment.reporting.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeUseCase;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** {@code discrepancy.events.v1} dinleyicisi; karşılaştırma sonucunu ve açık uyuşmazlıkları okuma modeline yansıtır. */
@Component
final class DiscrepancyEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ApplyReadModelChangeUseCase applyChange;

    DiscrepancyEventsListener(ApplyReadModelChangeUseCase applyChange) {
        this.applyChange = applyChange;
    }

    @KafkaListener(topics = DiscrepancyEvents.TOPIC)
    void onDiscrepancyEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        ReadModelChange change = changeOf(event);
        if (change != null) {
            applyChange.applyChange(new ApplyReadModelChangeCommand(event.id(), event.type(), change));
        }
    }

    /** Bu servisin kullanmadığı olay için {@code null}. */
    private static @Nullable ReadModelChange changeOf(IncomingEvent event) {
        return switch (event.type()) {
            case ComparisonCompleted.TYPE -> compared(event);
            case DiscrepancyOpened.TYPE -> opened(event);
            case DiscrepancyResolved.TYPE -> resolved(event);
            default -> null;
        };
    }

    private static ReadModelChange compared(IncomingEvent event) {
        ComparisonCompleted completed = EVENT_READER.readValue(event.data(), ComparisonCompleted.class);
        return new ReadModelChange.ComparisonCompleted(
                completed.shipmentId(),
                completed.comparedAt(),
                completed.quantityBand(),
                completed.diffKg(),
                completed.diffPct(),
                completed.discrepancyCount());
    }

    private static ReadModelChange opened(IncomingEvent event) {
        DiscrepancyOpened opened = EVENT_READER.readValue(event.data(), DiscrepancyOpened.class);
        return new ReadModelChange.DiscrepancyOpened(
                opened.discrepancyId(),
                opened.discrepancyNo(),
                opened.shipmentId(),
                opened.siteId(),
                opened.type(),
                opened.severity(),
                opened.dueAt(),
                event.time());
    }

    private static ReadModelChange resolved(IncomingEvent event) {
        DiscrepancyResolved resolved = EVENT_READER.readValue(event.data(), DiscrepancyResolved.class);
        return new ReadModelChange.DiscrepancyResolved(
                resolved.discrepancyId(), resolved.shipmentId(), resolved.resolvedAt(), resolved.remainingOpen());
    }
}
