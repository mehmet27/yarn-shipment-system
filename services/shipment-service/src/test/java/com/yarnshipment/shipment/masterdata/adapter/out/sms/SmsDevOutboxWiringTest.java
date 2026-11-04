package com.yarnshipment.shipment.masterdata.adapter.out.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.mockito.Mockito;

/** Düz kodun yalnızca provider=log VE local profilinde yazılabildiğini garanti eder. */
class SmsDevOutboxWiringTest {

    @Configuration
    @Import({LogSmsAdapter.class, UnconfiguredSmsAdapter.class, LocalSmsDevOutbox.class})
    static class Config {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }
    }

    private ApplicationContextRunner runner(String... props) {
        return new ApplicationContextRunner().withUserConfiguration(Config.class).withPropertyValues(props);
    }

    @Test
    @DisplayName("log + local profil: dev outbox yazıcısı vardır")
    void shouldWriteOnlyWithLogAndLocal() {
        runner("yarnshipment.sms.provider=log", "spring.profiles.active=local")
                .run(ctx -> assertThat(ctx).hasSingleBean(LocalSmsDevOutbox.class));
    }

    @Test
    @DisplayName("log ama local profil yok: yazıcı yoktur")
    void shouldNotWriteWithoutLocalProfile() {
        runner("yarnshipment.sms.provider=log").run(ctx -> assertThat(ctx).doesNotHaveBean(LocalSmsDevOutbox.class));
    }

    @Test
    @DisplayName("local profil ama provider none: yazıcı yoktur")
    void shouldNotWriteWithProviderNone() {
        runner("yarnshipment.sms.provider=none", "spring.profiles.active=local")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(LocalSmsDevOutbox.class));
    }

    @Test
    @DisplayName("local profil ama provider verilmemiş (varsayılan): yazıcı yoktur")
    void shouldNotWriteByDefault() {
        runner("spring.profiles.active=local").run(ctx -> assertThat(ctx).doesNotHaveBean(LocalSmsDevOutbox.class));
    }

    @Test
    @DisplayName("local profil ama gerçek/bilinmeyen sağlayıcı: yazıcı yoktur")
    void shouldNotWriteWithRealProvider() {
        runner("yarnshipment.sms.provider=netgsm", "spring.profiles.active=local")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(LocalSmsDevOutbox.class));
    }
}
