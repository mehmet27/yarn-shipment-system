package com.yarnshipment.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Outbox yayıncısının ayarları. {@code topic} verilmeyen serviste yayıncı açılmaz.
 *
 * @param topic olayların yazıldığı konu, ör. {@code shipment.events.v1} (EVT-04)
 * @param source CloudEvents {@code source} alanı, ör. {@code /yarn-shipment/shipment-service} (EVT-02)
 * @param pollInterval iki yoklama arasındaki bekleme
 * @param batchSize bir yoklamada en fazla gönderilen olay
 * @param sendTimeout Kafka onayının en fazla beklendiği süre
 */
@ConfigurationProperties(prefix = "yarnshipment.outbox")
public record OutboxProperties(
        String topic,
        String source,
        @DefaultValue("1s") Duration pollInterval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("10s") Duration sendTimeout) {}
