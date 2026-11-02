package com.yarnshipment.bag.adapter.in.messaging;

import com.yarnshipment.bag.application.port.in.RecordLotQualityLockCommand;
import com.yarnshipment.bag.application.port.in.RecordLotQualityLockUseCase;
import com.yarnshipment.bag.domain.model.LotQualityLock;
import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.masterdata.LotQualityLocked;
import com.yarnshipment.events.shipment.ShipmentEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ana veri olaylarının dinleyicisi. Tanımlar ayrı master-data servisine taşınana kadar shipment-service'te yönetildiği
 * için olaylar {@code shipment.events.v1} konusunda gelir; yalnızca parti kalite kilidi işlenir, diğer olaylar
 * onaylanır.
 */
@Component
final class MasterDataEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RecordLotQualityLockUseCase recordLotQualityLockUseCase;

    MasterDataEventsListener(RecordLotQualityLockUseCase recordLotQualityLockUseCase) {
        this.recordLotQualityLockUseCase = recordLotQualityLockUseCase;
    }

    @KafkaListener(topics = ShipmentEvents.TOPIC)
    void onShipmentTopicEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (!LotQualityLocked.TYPE.equals(event.type())) {
            return;
        }
        LotQualityLocked locked = EVENT_READER.readValue(event.data(), LotQualityLocked.class);
        recordLotQualityLockUseCase.recordLotQualityLock(new RecordLotQualityLockCommand(
                event.id(),
                event.type(),
                new LotQualityLock(
                        locked.lotId(),
                        locked.siteId(),
                        locked.lotNo(),
                        locked.productId(),
                        locked.qualityGradeCode(),
                        locked.lockedAt())));
    }
}
