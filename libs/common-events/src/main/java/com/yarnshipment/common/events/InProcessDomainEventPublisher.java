package com.yarnshipment.common.events;

import com.yarnshipment.common.events.DomainEventConsumer.Delivery;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Olayı aynı süreçteki {@link DomainEventConsumer}'lara iletir (tek Spring Boot uygulaması). Konu ve kaynak, olay
 * türüne göre {@link EventRoutes}'tan gelir.
 *
 * <p>Teslim semantiği: {@link Delivery#BEFORE_COMMIT} tüketicileri üreten işlemin commit'inden hemen önce aynı
 * transaction'da çalışır (hata iş işlemini geri alır); {@link Delivery#AFTER_COMMIT} tüketicileri commit'ten sonra
 * kendi transaction'larında çalışır (hata loglanır). Aktif transaction yoksa ikisi de hemen çalışır. Bir tüketici
 * commit öncesi yeni olay yayınlarsa o olay da aynı transaction'da hemen iletilir.
 */
public final class InProcessDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InProcessDomainEventPublisher.class);
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);

    private final JsonMapper jsonMapper;
    private final Supplier<List<DomainEventConsumer>> consumers;
    private final TransactionTemplate afterCommit;
    private final EventRoutes routes;

    public InProcessDomainEventPublisher(
            JsonMapper jsonMapper,
            Supplier<List<DomainEventConsumer>> consumers,
            PlatformTransactionManager transactionManager,
            EventRoutes routes) {
        this.jsonMapper = jsonMapper;
        this.consumers = consumers;
        this.afterCommit = new TransactionTemplate(transactionManager);
        this.afterCommit.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.routes = routes;
    }

    @Override
    public void publish(DomainEvent event) {
        EventsProperties.Route route = routes.find(event.type())
                .orElseThrow(() -> new IllegalStateException(
                        "No yarnshipment.events.routes entry for event type " + event.type()));
        IncomingEvent incoming = new IncomingEvent(
                UuidV7.next(),
                event.type(),
                event.aggregateId(),
                event.occurredAt(),
                event.actorId().toString(),
                jsonMapper.writeValueAsString(event.payload()),
                route.topic(),
                route.source(),
                Map.of());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliver(incoming, Delivery.BEFORE_COMMIT);
            deliverAfterCommit(incoming);
        } else if (DISPATCHING.get()) {
            deliver(incoming, Delivery.BEFORE_COMMIT);
            TransactionSynchronizationManager.registerSynchronization(afterCommitOf(incoming));
        } else {
            TransactionSynchronizationManager.registerSynchronization(new BeforeCommit(incoming));
            TransactionSynchronizationManager.registerSynchronization(afterCommitOf(incoming));
        }
    }

    private TransactionSynchronization afterCommitOf(IncomingEvent incoming) {
        return new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliverAfterCommit(incoming);
            }
        };
    }

    private void deliver(IncomingEvent incoming, Delivery delivery) {
        for (DomainEventConsumer consumer : consumers.get()) {
            if (consumer.delivery() == delivery) {
                consumer.consume(incoming);
            }
        }
    }

    /** Commit sonrası tüketici hatası iş işlemini etkilemez; her tüketici ayrı korunur ve hata loglanır. */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void deliverAfterCommit(IncomingEvent incoming) {
        for (DomainEventConsumer consumer : consumers.get()) {
            if (consumer.delivery() != Delivery.AFTER_COMMIT) {
                continue;
            }
            try {
                afterCommit.executeWithoutResult(status -> consumer.consume(incoming));
            } catch (RuntimeException e) {
                LOG.error("After-commit event consumer failed type={} id={}", incoming.type(), incoming.id(), e);
            }
        }
    }

    /** Üreten işlemin commit'inden hemen önce BEFORE_COMMIT tüketicilerini çalıştırır. */
    private final class BeforeCommit implements TransactionSynchronization {

        private final IncomingEvent incoming;

        BeforeCommit(IncomingEvent incoming) {
            this.incoming = incoming;
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            DISPATCHING.set(true);
            try {
                deliver(incoming, Delivery.BEFORE_COMMIT);
            } finally {
                DISPATCHING.set(false);
            }
        }
    }
}
