package com.yarnshipment.common.outbox;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * EVT-03/05/09: servis veritabanı kullanıyorsa {@link ProcessedEvents} ve dinleyici hata politikası her zaman,
 * {@code yarnshipment.outbox.topic} verildiyse ayrıca yoklamalı yayıncı açılır. Servis {@code outbox_event} ve
 * {@code processed_event} tablolarını migration'ıyla oluşturur.
 */
@AutoConfiguration(
        after = {JdbcClientAutoConfiguration.class, KafkaAutoConfiguration.class, TransactionAutoConfiguration.class})
@ConditionalOnBean(JdbcClient.class)
public class OutboxAutoConfiguration {

    @Bean
    ProcessedEvents processedEvents(JdbcClient jdbcClient, ObjectProvider<Clock> clock) {
        return new ProcessedEvents(jdbcClient, clock.getIfAvailable(Clock::systemUTC));
    }

    /**
     * EVT-09: dinleyici hatasında üstel beklemeyle en fazla 3 tekrar, sonra {@code <konu>.DLT}. Seri kaldırma ve
     * doğrulama hataları tekrar edilmeden DLT'ye gider. Boot bu bean'i dinleyici konteynerine kendisi bağlar.
     */
    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        return KafkaErrorHandlers.deadLetterAfterRetries(kafka);
    }

    /**
     * Yayıncı yalnızca konu ayarlanmış serviste açılır; yalnızca tüketen serviste kapalı kalır. Kafka'sız çalışan
     * entegrasyon testleri {@code yarnshipment.outbox.enabled=false} ile kapatır.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "yarnshipment.outbox", name = "topic")
    @ConditionalOnBooleanProperty(name = "yarnshipment.outbox.enabled", matchIfMissing = true)
    @ConditionalOnBean({KafkaTemplate.class, PlatformTransactionManager.class})
    @EnableConfigurationProperties(OutboxProperties.class)
    static class RelayConfiguration {

        @Bean
        OutboxStore outboxStore(JdbcClient jdbcClient, JsonMapper jsonMapper, ObjectProvider<Clock> clock) {
            return new JdbcOutboxStore(jdbcClient, jsonMapper, clock.getIfAvailable(Clock::systemUTC));
        }

        @Bean
        OutboxRelay outboxRelay(
                OutboxStore store,
                KafkaTemplate<String, String> kafka,
                PlatformTransactionManager transactionManager,
                OutboxProperties properties) {
            return new OutboxRelay(store, kafka, new TransactionTemplate(transactionManager), properties);
        }

        @Bean
        OutboxPoller outboxPoller(OutboxRelay relay, OutboxProperties properties) {
            return new OutboxPoller(relay, properties.pollInterval());
        }
    }
}
