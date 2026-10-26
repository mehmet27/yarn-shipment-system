package com.yarnshipment.common.outbox;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Yoklamalı outbox yayıncısı (Faz 1, mimari doküman). Bir turda olaylar kilitlenir, sırayla Kafka'ya gönderilir ve
 * Kafka onayından sonra gönderildi işaretlenir. Gönderim hatasında transaction geri alınır, olaylar bir sonraki turda
 * yeniden gönderilir: teslim en az bir kezdir, tüketiciler idempotenttir (EVT-05).
 */
public final class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transactions;
    private final OutboxProperties properties;

    public OutboxRelay(
            OutboxStore store,
            KafkaTemplate<String, String> kafka,
            TransactionTemplate transactions,
            OutboxProperties properties) {
        this.store = store;
        this.kafka = kafka;
        this.transactions = transactions;
        this.properties = properties;
    }

    /** Bir tur çalışır ve gönderilen olay sayısını döner; hata olursa sıfır. */
    public int publishPending() {
        try {
            Integer published = transactions.execute(status -> publishBatch());
            return published == null ? 0 : published;
        } catch (OutboxPublishException e) {
            LOG.warn("Outbox publish failed, will retry topic={}", properties.topic(), e);
            return 0;
        }
    }

    private int publishBatch() {
        List<OutboxEvent> events = store.lockUnpublished(properties.batchSize());
        for (OutboxEvent event : events) {
            send(CloudEventHeaders.toRecord(properties.topic(), properties.source(), event));
        }
        if (!events.isEmpty()) {
            store.markPublished(events);
        }
        return events.size();
    }

    private void send(ProducerRecord<String, String> record) {
        CompletableFuture<SendResult<String, String>> result = kafka.send(record);
        try {
            result.get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxPublishException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new OutboxPublishException(e);
        }
    }

    /** Kafka gönderimi başarısız; tur geri alınır. */
    static final class OutboxPublishException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        OutboxPublishException(Throwable cause) {
            super(cause);
        }
    }
}
