package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

class OutboxPollerTest {

    private static final int PENDING = 3;
    private static final Duration INTERVAL = Duration.ofMillis(20);
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final long STEP_MILLIS = 10;

    @Test
    @DisplayName("Uygulama açıkken bekleyen olaylar arka planda gönderilir; durdurunca döngü kapanır")
    @SuppressWarnings("unchecked")
    void shouldDrainInBackgroundUntilStopped() throws InterruptedException {
        // given
        InMemoryOutboxStore store = new InMemoryOutboxStore();
        for (int i = 0; i < PENDING; i++) {
            store.add(TestEvents.event(UUID.randomUUID()));
        }
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(ArgumentMatchers.<ProducerRecord<String, String>>any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        OutboxProperties properties = new OutboxProperties(
                "shipment.events.v1", "/yarn-shipment/shipment-service", INTERVAL, 1, Duration.ofSeconds(1));
        OutboxPoller poller = new OutboxPoller(
                new OutboxRelay(store, kafka, new TransactionTemplate(new RecordingTransactionManager()), properties),
                properties.pollInterval());

        // when
        poller.start();
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (!store.pending().isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(STEP_MILLIS);
        }
        poller.stop();

        // then
        assertThat(store.published()).hasSize(PENDING);
        assertThat(poller.isRunning()).isFalse();
    }
}
