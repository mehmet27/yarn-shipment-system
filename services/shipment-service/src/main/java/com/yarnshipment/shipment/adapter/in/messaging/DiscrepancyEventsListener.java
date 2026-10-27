package com.yarnshipment.shipment.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckUseCase;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewUseCase;
import com.yarnshipment.shipment.domain.check.ComparisonOutcome;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code discrepancy.events.v1} dinleyicisi. Karşılaştırma sonucu sevkiyatı tamamlar ya da incelemeye alır; tüm
 * uyuşmazlıklar çözülünce incelemedeki sevkiyatı tamamlar. Diğer olaylar durumu değiştirmez.
 */
@Component
final class DiscrepancyEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ConcludeCheckUseCase concludeCheckUseCase;
    private final ConcludeReviewUseCase concludeReviewUseCase;

    DiscrepancyEventsListener(ConcludeCheckUseCase concludeCheckUseCase, ConcludeReviewUseCase concludeReviewUseCase) {
        this.concludeCheckUseCase = concludeCheckUseCase;
        this.concludeReviewUseCase = concludeReviewUseCase;
    }

    @KafkaListener(topics = DiscrepancyEvents.TOPIC)
    void onDiscrepancyEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (DiscrepancyResolved.TYPE.equals(event.type())) {
            onResolved(event);
        } else if (ComparisonCompleted.TYPE.equals(event.type())) {
            onComparisonCompleted(event);
        }
    }

    /** Sevkiyatın son açık uyuşmazlığı çözüldüyse inceleme biter; kalan varsa sevkiyat incelemede kalır. */
    private void onResolved(IncomingEvent event) {
        DiscrepancyResolved resolved = EVENT_READER.readValue(event.data(), DiscrepancyResolved.class);
        if (resolved.allResolved()) {
            concludeReviewUseCase.concludeReview(
                    new ConcludeReviewCommand(event.id(), event.type(), resolved.shipmentId()));
        }
    }

    private void onComparisonCompleted(IncomingEvent event) {
        ComparisonCompleted comparison = EVENT_READER.readValue(event.data(), ComparisonCompleted.class);
        ComparisonOutcome outcome = comparison.clean() ? ComparisonOutcome.CLEAN : ComparisonOutcome.DISCREPANCY;
        concludeCheckUseCase.concludeCheck(
                new ConcludeCheckCommand(event.id(), event.type(), comparison.shipmentId(), outcome));
    }
}
