package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxRelayTest {

    private static final OutboxProperties PROPERTIES = new OutboxProperties(
            "shipment.events.v1", "/yarn-shipment/shipment-service", Duration.ofSeconds(1), 2, Duration.ofSeconds(1));

    private static final int PENDING = 3;

    private final InMemoryOutboxStore store = new InMemoryOutboxStore();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private final OutboxRelay relay =
            new OutboxRelay(store, kafka, new TransactionTemplate(transactionManager), PROPERTIES);

    @Test
    @DisplayName("Bekleyen olaylar parti boyunca gönderilir ve Kafka onayından sonra işaretlenir")
    void shouldPublishBatchAndMarkPublished() {
        // given
        store.add(TestEvents.event(UUID.randomUUID()));
        store.add(TestEvents.event(UUID.randomUUID()));
        store.add(TestEvents.event(UUID.randomUUID()));
        when(kafka.send(ArgumentMatchers.<ProducerRecord<String, String>>any())).thenReturn(acknowledged());

        // when
        int first = relay.publishPending();
        int second = relay.publishPending();

        // then
        assertThat(first).isEqualTo(2);
        assertThat(second).isEqualTo(1);
        assertThat(store.published()).hasSize(PENDING);
        verify(kafka, times(PENDING)).send(ArgumentMatchers.<ProducerRecord<String, String>>any());
        assertThat(transactionManager.commits()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kafka gönderimi başarısızsa olaylar işaretlenmez ve transaction geri alınır")
    void shouldKeepEventsWhenSendFails() {
        // given
        store.add(TestEvents.event(UUID.randomUUID()));
        when(kafka.send(ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        // when
        int published = relay.publishPending();

        // then
        assertThat(published).isZero();
        assertThat(store.pending()).hasSize(1);
        assertThat(store.published()).isEmpty();
        assertThat(transactionManager.rollbacks()).isEqualTo(1);
    }

    @Test
    @DisplayName("Bekleyen olay yoksa Kafka çağrılmaz")
    void shouldDoNothingWhenEmpty() {
        // when
        int published = relay.publishPending();

        // then
        assertThat(published).isZero();
        verify(kafka, times(0)).send(ArgumentMatchers.<ProducerRecord<String, String>>any());
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<SendResult<String, String>> acknowledged() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }
}
