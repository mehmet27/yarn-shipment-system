package com.yarnshipment.shipment.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckUseCase;
import com.yarnshipment.shipment.domain.check.ComparisonOutcome;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code discrepancy.events.v1} dinleyicisi. Karşılaştırma sonucu sevkiyatı tamamlar ya da incelemeye alır; diğer
 * uyuşmazlık olayları sevkiyat durumunu değiştirmez.
 */
@Component
final class DiscrepancyEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ConcludeCheckUseCase concludeCheckUseCase;

    DiscrepancyEventsListener(ConcludeCheckUseCase concludeCheckUseCase) {
        this.concludeCheckUseCase = concludeCheckUseCase;
    }

    @KafkaListener(topics = DiscrepancyEvents.TOPIC)
    void onDiscrepancyEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (!ComparisonCompleted.TYPE.equals(event.type())) {
            return;
        }
        ComparisonCompleted comparison = EVENT_READER.readValue(event.data(), ComparisonCompleted.class);
        ComparisonOutcome outcome = comparison.clean() ? ComparisonOutcome.CLEAN : ComparisonOutcome.DISCREPANCY;
        concludeCheckUseCase.concludeCheck(
                new ConcludeCheckCommand(event.id(), event.type(), comparison.shipmentId(), outcome));
    }
}
