package com.yarnshipment.common.events.testing;

import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Testlerde yayınlanan olayları tutan yayıncı. Gerçek taşıyıcı (süreç içi tüketiciler) olmadan, bir modülün hangi
 * olayı yayınladığını doğrulamak için kullanılır.
 */
public final class RecordingDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    /** Yayınlanan tüm olaylar, yayın sırasıyla. */
    public List<DomainEvent> published() {
        return List.copyOf(events);
    }

    /** Belirtilen kök ve türdeki olay sayısı. */
    public long count(UUID aggregateId, String type) {
        return events.stream()
                .filter(e -> e.aggregateId().equals(aggregateId) && e.type().equals(type))
                .count();
    }

    /** Belirtilen türdeki olay sayısı. */
    public long count(String type) {
        return events.stream().filter(e -> e.type().equals(type)).count();
    }

    public void clear() {
        events.clear();
    }
}
