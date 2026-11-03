package com.yarnshipment.audit.adapter.out.persistence;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * İki ayrı veritabanı bağlantısı: yazan rol ({@code audit_writer}) yalnızca {@code audit_append()} çalıştırır, okuma
 * rolü ({@code audit_reader}) yalnızca SELECT yapar. Okuma bağlantısı {@link DataSource} bean'i olmaz; böylece
 * Spring Boot'un birincil bağlantısı yazan roldür.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditReaderProperties.class)
class AuditJdbcConfiguration {

    static final String READER = "auditReader";

    @Bean
    @Primary
    JdbcClient auditWriterJdbc(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean(READER)
    JdbcClient auditReaderJdbc(AuditReaderProperties properties) {
        DataSource readerDataSource = DataSourceBuilder.create()
                .url(properties.url())
                .username(properties.username())
                .password(properties.password())
                .build();
        return JdbcClient.create(readerDataSource);
    }
}
