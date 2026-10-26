package com.yarnshipment.common.outbox;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.core.JacksonException;

/** Tüketici hata politikası (EVT-09). */
public final class KafkaErrorHandlers {

    /** Geçici hatada tekrar sayısı. */
    public static final int MAX_RETRIES = 3;

    private static final long INITIAL_INTERVAL_MS = 500;
    private static final double MULTIPLIER = 2.0;
    private static final String DLT_SUFFIX = ".DLT";

    private KafkaErrorHandlers() {}

    /** Üstel beklemeyle {@link #MAX_RETRIES} tekrar, sonra DLT; DLT'nin bölümünü Kafka seçer. */
    public static DefaultErrorHandler deadLetterAfterRetries(KafkaTemplate<String, String> kafka) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1));
        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_INTERVAL_MS, MULTIPLIER);
        backOff.setMaxAttempts(MAX_RETRIES);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                DeserializationException.class, JacksonException.class, IllegalArgumentException.class);
        return handler;
    }
}
