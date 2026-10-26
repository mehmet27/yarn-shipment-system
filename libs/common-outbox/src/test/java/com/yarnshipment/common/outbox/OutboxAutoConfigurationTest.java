package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JdbcClient.class, () -> mock(JdbcClient.class))
            .withBean(JsonMapper.class, JsonMapper::shared)
            .withBean(PlatformTransactionManager.class, RecordingTransactionManager::new)
            .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class));

    @Test
    @DisplayName("Konu verilmezse yalnızca idempotent tüketici yardımcısı açılır")
    void shouldSkipRelayWithoutTopic() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ProcessedEvents.class);
            assertThat(context).hasSingleBean(DefaultErrorHandler.class);
            assertThat(context).doesNotHaveBean(OutboxRelay.class);
        });
    }

    @Test
    @DisplayName("Konu verilince yayıncı ve yoklayıcı açılır")
    void shouldStartRelayWithTopic() {
        runner.withPropertyValues(
                        "yarnshipment.outbox.topic=shipment.events.v1",
                        "yarnshipment.outbox.source=/yarn-shipment/shipment-service",
                        "yarnshipment.outbox.poll-interval=1h")
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxRelay.class);
                    assertThat(context).hasSingleBean(OutboxPoller.class);
                });
    }

    @Test
    @DisplayName("Konu verilse de enabled=false ile yayıncı kapalı kalır")
    void shouldSkipRelayWhenDisabled() {
        runner.withPropertyValues(
                        "yarnshipment.outbox.topic=shipment.events.v1",
                        "yarnshipment.outbox.source=/yarn-shipment/shipment-service",
                        "yarnshipment.outbox.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OutboxRelay.class));
    }

    @Test
    @DisplayName("Veritabanı olmayan uygulamada hiçbir bean açılmaz")
    void shouldSkipWithoutJdbcClient() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(ProcessedEvents.class));
    }
}
