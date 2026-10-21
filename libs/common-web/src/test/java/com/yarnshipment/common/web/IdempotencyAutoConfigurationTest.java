package com.yarnshipment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.web.config.IdempotencyAutoConfiguration;
import com.yarnshipment.common.web.config.WebComponentsConfiguration;
import com.yarnshipment.common.web.idempotency.IdempotencyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.json.JsonMapper;

class IdempotencyAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(WebComponentsConfiguration.class)
            .withBean("messageSource", StaticMessageSource.class)
            .withBean(JsonMapper.class, JsonMapper::shared)
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class));

    @Test
    @DisplayName("JdbcClient varsa Idempotency-Key filtresi ve kayıt defteri eklenir")
    void shouldRegisterFilterWhenJdbcClientPresent() {
        runner.withBean(JdbcClient.class, () -> JdbcClient.create(new DriverManagerDataSource("jdbc:postgresql://x/y")))
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotencyStore.class);
                    assertThat(context)
                            .getBeanNames(FilterRegistrationBean.class)
                            .hasSize(2);
                });
    }

    @Test
    @DisplayName("JdbcClient yoksa yalnızca istek kimliği filtresi eklenir")
    void shouldSkipIdempotencyWithoutJdbcClient() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(IdempotencyStore.class);
            assertThat(context).getBeanNames(FilterRegistrationBean.class).hasSize(1);
        });
    }
}
