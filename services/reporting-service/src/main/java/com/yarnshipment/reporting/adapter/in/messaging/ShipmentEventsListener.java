package com.yarnshipment.reporting.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.shipment.ShipmentEvents;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeUseCase;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** {@code shipment.events.v1} dinleyicisi; sevkiyatın durum ve kilo bilgilerini okuma modeline yansıtır. */
@Component
final class ShipmentEventsListener {

    private final ApplyReadModelChangeUseCase applyChange;

    ShipmentEventsListener(ApplyReadModelChangeUseCase applyChange) {
        this.applyChange = applyChange;
    }

    @KafkaListener(topics = ShipmentEvents.TOPIC)
    void onShipmentEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        ReadModelChange change = ShipmentChangeMapper.toChange(event);
        if (change != null) {
            applyChange.applyChange(new ApplyReadModelChangeCommand(event.id(), event.type(), change));
        }
    }
}
