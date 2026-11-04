package com.yarnshipment.audit.adapter.out.persistence;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * İki ayrı bağlantı havuzu (yazma/audit zinciri ve audit ekranı okuması). Tek veritabanı kullanıcısına geçildiği için
 * ikisi de aynı kullanıcıyla bağlanır; "yazan rol tabloyu okuyamaz" koruması artık yoktur. Okuma bağlantısı
 * {@link DataSource} bean'i olmaz; Spring Boot'un birincil bağlantısı yazma tarafıdır.
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
        HikariDataSource readerDataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(properties.url())
                .username(properties.username())
                .password(properties.password())
                .build();
        readerDataSource.setSchema(properties.schema());
        return JdbcClient.create(readerDataSource);
    }
}
