package com.yarnshipment.common.events;

import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Süreç içi olay yayıncısı: işlem yöneticisi olan her uygulamada {@link DomainEventPublisher} olarak açılır.
 * Tüketiciler {@link DomainEventConsumer} bean'leridir.
 */
@AutoConfiguration(after = TransactionAutoConfiguration.class)
@EnableConfigurationProperties(EventsProperties.class)
@ConditionalOnBean(PlatformTransactionManager.class)
public class EventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher inProcessDomainEventPublisher(
            JsonMapper jsonMapper,
            ObjectProvider<DomainEventConsumer> consumers,
            PlatformTransactionManager transactionManager,
            EventsProperties properties) {
        return new InProcessDomainEventPublisher(
                jsonMapper,
                () -> consumers.orderedStream().toList(),
                transactionManager,
                new EventRoutes(properties.routes()));
    }
}
