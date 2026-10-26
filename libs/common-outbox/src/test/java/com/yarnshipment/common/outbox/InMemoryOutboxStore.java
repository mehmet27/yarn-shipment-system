package com.yarnshipment.common.outbox;

import java.util.ArrayList;
import java.util.List;

/** Bellekte outbox; gönderildi işaretlenen olaylar listeden düşer. */
final class InMemoryOutboxStore implements OutboxStore {

    private final List<OutboxEvent> pending = new ArrayList<>();
    private final List<OutboxEvent> published = new ArrayList<>();

    void add(OutboxEvent event) {
        pending.add(event);
    }

    List<OutboxEvent> published() {
        return published;
    }

    List<OutboxEvent> pending() {
        return pending;
    }

    @Override
    public synchronized List<OutboxEvent> lockUnpublished(int limit) {
        return List.copyOf(pending.subList(0, Math.min(limit, pending.size())));
    }

    @Override
    public synchronized void markPublished(List<OutboxEvent> events) {
        pending.removeAll(events);
        published.addAll(events);
    }
}
