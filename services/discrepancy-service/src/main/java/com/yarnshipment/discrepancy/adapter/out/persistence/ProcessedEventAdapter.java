package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.common.outbox.ProcessedEvents;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link ProcessedEventPort}'u ortak {@code processed_event} kaydıyla uygular. */
@Component
final class ProcessedEventAdapter implements ProcessedEventPort {

    private final ProcessedEvents processedEvents;

    ProcessedEventAdapter(ProcessedEvents processedEvents) {
        this.processedEvents = processedEvents;
    }

    @Override
    public boolean markProcessed(UUID eventId, String eventType) {
        return processedEvents.markProcessed(eventId, eventType);
    }
}
