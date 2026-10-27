package com.yarnshipment.evidence.adapter.out.ticket;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.yarnshipment.evidence.application.port.out.TicketReaderPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Okuma açıksa Claude okuyucusunu, kapalıysa her isteği reddeden okuyucuyu kurar. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TicketReadingProperties.class)
class TicketReaderConfiguration {

    /** Bir yeniden deneme; kullanıcı ekranda beklediği için daha fazlası okumayı geciktirir. */
    private static final int MAX_RETRIES = 1;

    @Bean
    TicketReaderPort ticketReader(TicketReadingProperties properties, JsonMapper jsonMapper) {
        if (!properties.enabled()) {
            return new DisabledTicketReader();
        }
        return new ClaudeTicketReader(
                AnthropicOkHttpClient.builder()
                        .apiKey(properties.apiKey())
                        .timeout(properties.timeout())
                        .maxRetries(MAX_RETRIES)
                        .build(),
                properties.model(),
                new TicketScanParser(jsonMapper));
    }
}
