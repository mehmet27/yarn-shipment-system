package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.outbox.DomainEventConsumer.Delivery;
import com.yarnshipment.moduleapi.event.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class InProcessDomainEventPublisherTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final String TYPE = "com.yarnshipment.shipment.shipment-created.v1";

    private final List<String> log = new ArrayList<>();
    private final FakeTransactionManager transactions = new FakeTransactionManager(log);
    private final TransactionTemplate template = new TransactionTemplate(transactions);

    private InProcessDomainEventPublisher publisher(DomainEventConsumer... consumers) {
        return new InProcessDomainEventPublisher(
                JsonMapper.builder().build(),
                () -> List.of(consumers),
                transactions,
                "shipment.events.v1",
                "/yarn-shipment/shipment-service");
    }

    private static DomainEvent event() {
        return new DomainEvent(
                "Shipment", SHIPMENT, TYPE, new Payload("SV-1"), Instant.parse("2026-11-12T10:00:00Z"), ACTOR);
    }

    @Test
    @DisplayName(
            "BEFORE_COMMIT tüketicisi üreten işlemin commit'inden hemen önce çalışır; AFTER_COMMIT sonra, yeni işlemde")
    void shouldDeliverAroundCommit() {
        InProcessDomainEventPublisher publisher =
                publisher(consumer(Delivery.BEFORE_COMMIT, "before"), consumer(Delivery.AFTER_COMMIT, "after"));

        template.executeWithoutResult(status -> {
            log.add("business");
            publisher.publish(event());
            log.add("business-done");
        });

        assertThat(log)
                .containsExactly("begin", "business", "business-done", "before", "commit", "begin", "after", "commit");
    }

    @Test
    @DisplayName("Olay zarfı ve gövdesi tüketiciye taşıyıcıdan bağımsız IncomingEvent olarak gelir")
    void shouldBuildIncomingEvent() {
        List<IncomingEvent> received = new ArrayList<>();
        InProcessDomainEventPublisher publisher = publisher(new DomainEventConsumer() {
            @Override
            public Delivery delivery() {
                return Delivery.BEFORE_COMMIT;
            }

            @Override
            public void consume(IncomingEvent event) {
                received.add(event);
            }
        });

        publisher.publish(event());

        assertThat(received).singleElement().satisfies(in -> {
            assertThat(in.type()).isEqualTo(TYPE);
            assertThat(in.subject()).isEqualTo(SHIPMENT);
            assertThat(in.actorId()).isEqualTo(ACTOR.toString());
            assertThat(in.data()).isEqualTo("{\"no\":\"SV-1\"}");
            assertThat(in.topic()).isEqualTo("shipment.events.v1");
            assertThat(in.source()).isEqualTo("/yarn-shipment/shipment-service");
        });
    }

    @Test
    @DisplayName("BEFORE_COMMIT tüketicisi hata verirse iş işlemi geri alınır ve AFTER_COMMIT çalışmaz")
    void shouldRollBackWhenBeforeCommitConsumerFails() {
        InProcessDomainEventPublisher publisher =
                publisher(failing(Delivery.BEFORE_COMMIT), consumer(Delivery.AFTER_COMMIT, "after"));

        assertThatThrownBy(() -> template.executeWithoutResult(status -> publisher.publish(event())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(log).containsExactly("begin", "rollback");
    }

    @Test
    @DisplayName("AFTER_COMMIT tüketicisi hata verirse iş işlemi etkilenmez, sonraki tüketici yine çalışır")
    void shouldIsolateAfterCommitFailures() {
        InProcessDomainEventPublisher publisher =
                publisher(failing(Delivery.AFTER_COMMIT), consumer(Delivery.AFTER_COMMIT, "second"));

        template.executeWithoutResult(status -> publisher.publish(event()));

        assertThat(log).containsExactly("begin", "commit", "begin", "rollback", "begin", "second", "commit");
    }

    @Test
    @DisplayName("Aktif işlem yoksa tüketiciler hemen çalışır")
    void shouldDeliverImmediatelyWithoutTransaction() {
        InProcessDomainEventPublisher publisher =
                publisher(consumer(Delivery.BEFORE_COMMIT, "before"), consumer(Delivery.AFTER_COMMIT, "after"));

        publisher.publish(event());

        assertThat(log).containsExactly("before", "begin", "after", "commit");
    }

    @Test
    @DisplayName("Commit öncesi tüketicinin yayınladığı olay aynı işlemde hemen iletilir")
    void shouldDeliverCascadedEventInSameTransaction() {
        List<String> cascade = new ArrayList<>();
        InProcessDomainEventPublisher[] holder = new InProcessDomainEventPublisher[1];
        DomainEventConsumer first = new DomainEventConsumer() {
            @Override
            public Delivery delivery() {
                return Delivery.BEFORE_COMMIT;
            }

            @Override
            public void consume(IncomingEvent event) {
                cascade.add("first:" + event.type());
                if (cascade.size() == 1) {
                    holder[0].publish(
                            new DomainEvent("Shipment", SHIPMENT, TYPE + "x", new Payload("n"), Instant.EPOCH, ACTOR));
                }
            }
        };
        holder[0] = publisher(first);

        template.executeWithoutResult(status -> holder[0].publish(event()));

        assertThat(cascade).containsExactly("first:" + TYPE, "first:" + TYPE + "x");
        assertThat(log).containsExactly("begin", "commit");
    }

    private DomainEventConsumer consumer(Delivery delivery, String name) {
        return new DomainEventConsumer() {
            @Override
            public Delivery delivery() {
                return delivery;
            }

            @Override
            public void consume(IncomingEvent event) {
                log.add(name);
            }
        };
    }

    private static DomainEventConsumer failing(Delivery delivery) {
        return new DomainEventConsumer() {
            @Override
            public Delivery delivery() {
                return delivery;
            }

            @Override
            public void consume(IncomingEvent event) {
                throw new IllegalStateException("consumer failed");
            }
        };
    }

    private record Payload(String no) {}

    /** İşlem sınırlarını kaydeden, gerçek veritabanı gerektirmeyen yönetici. */
    private static final class FakeTransactionManager extends AbstractPlatformTransactionManager {

        private static final long serialVersionUID = 1L;
        private final transient List<String> log;

        FakeTransactionManager(List<String> log) {
            this.log = log;
            setTransactionSynchronization(SYNCHRONIZATION_ALWAYS);
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            log.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            log.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            log.add("rollback");
        }
    }
}
