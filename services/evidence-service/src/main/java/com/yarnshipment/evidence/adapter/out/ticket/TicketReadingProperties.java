package com.yarnshipment.evidence.adapter.out.ticket;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fiş okuma ayarları. Kapalıyken okuma ucu 503 döner ve istemci okumasız devam eder.
 *
 * @param enabled okuma açık mı
 * @param model görüntü modeli, ör. {@code claude-sonnet-5-5}
 * @param apiKey model API anahtarı; yalnızca ortam değişkeninden gelir (SEC-14)
 * @param timeout tek okumanın üst süresi; kullanıcı önizleme ekranında bekler
 */
@ConfigurationProperties("yarnshipment.evidence.ticket-reading")
record TicketReadingProperties(boolean enabled, String model, String apiKey, Duration timeout) {}
