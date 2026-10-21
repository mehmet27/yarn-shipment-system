package com.yarnshipment.common.web.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Idempotency-Key saklama süreleri.
 *
 * @param completedRetention tamamlanmış kaydın ne kadar tekrar için tutulacağı
 * @param inProgressTimeout çöken bir isteğin açık bıraktığı anahtarın ne zaman serbest sayılacağı
 */
@ConfigurationProperties(prefix = "yarnshipment.idempotency")
public record IdempotencyProperties(
        @DefaultValue("24h") Duration completedRetention,
        @DefaultValue("2m") Duration inProgressTimeout) {}
