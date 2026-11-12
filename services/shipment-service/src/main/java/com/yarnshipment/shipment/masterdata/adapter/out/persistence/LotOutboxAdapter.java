package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.events.masterdata.LotQualityLocked;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import com.yarnshipment.shipment.masterdata.application.port.out.LotEventPort;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Parti olaylarını {@link DomainEventPublisher} ile yayınlar (EVT-03); sevkiyat olaylarıyla aynı konuya gider. Paket
 * ayrı master-data servisine taşınacağı için sevkiyat adapter'larına bağlanmaz.
 */
@Component
final class LotOutboxAdapter implements LotEventPort {

    private static final String AGGREGATE_TYPE = "ProductionLot";

    private final DomainEventPublisher events;

    LotOutboxAdapter(DomainEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publishQualityLocked(UUID actorId, ProductionLot lot) {
        Instant lockedAt = Objects.requireNonNull(lot.qualityLockedAt(), "qualityLockedAt");
        LotQualityLocked event = new LotQualityLocked(
                lot.id(),
                lot.siteId(),
                lot.details().lotNo(),
                lot.details().productId(),
                lot.labels().qualityGradeCode(),
                lockedAt);
        events.publish(new DomainEvent(AGGREGATE_TYPE, lot.id(), LotQualityLocked.TYPE, event, lockedAt, actorId));
    }
}
