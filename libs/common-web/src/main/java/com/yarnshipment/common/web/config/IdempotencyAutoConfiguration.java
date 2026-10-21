package com.yarnshipment.common.web.config;

import com.yarnshipment.common.web.idempotency.IdempotencyFilter;
import com.yarnshipment.common.web.idempotency.IdempotencyProperties;
import com.yarnshipment.common.web.idempotency.IdempotencyStore;
import com.yarnshipment.common.web.idempotency.JdbcIdempotencyStore;
import com.yarnshipment.common.web.problem.ProblemFactory;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * API-04: servis veritabanı kullanıyorsa Idempotency-Key filtresi kendiliğinden eklenir. Otomatik yapılandırma
 * olduğu için dilimli web testlerine ({@code @WebMvcTest}) girmez; servis {@code idempotency_record} tablosunu
 * migration'ıyla oluşturur. Filtre, Spring Security zincirinden sonra çalışır.
 */
@AutoConfiguration(after = JdbcClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({JdbcClient.class, ProblemFactory.class})
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    /** Spring Security filtre zinciri -100'dedir; idempotency kullanıcıyı tanıdıktan sonra çalışmalıdır. */
    private static final int AFTER_SECURITY_ORDER = -90;

    @Bean
    IdempotencyStore idempotencyStore(
            JdbcClient jdbcClient, ObjectProvider<Clock> clock, IdempotencyProperties properties) {
        return new JdbcIdempotencyStore(jdbcClient, clock.getIfAvailable(Clock::systemUTC), properties);
    }

    @Bean
    FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(
            IdempotencyStore store, ProblemFactory problems, JsonMapper jsonMapper) {
        FilterRegistrationBean<IdempotencyFilter> registration =
                new FilterRegistrationBean<>(new IdempotencyFilter(store, problems, jsonMapper));
        registration.setOrder(AFTER_SECURITY_ORDER);
        return registration;
    }
}
