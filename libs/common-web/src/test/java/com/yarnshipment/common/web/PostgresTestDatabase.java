package com.yarnshipment.common.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Testler boyunca bir kez başlatılan PostgreSQL; {@code idempotency_record} referans şemasıyla hazırdır. */
final class PostgresTestDatabase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("mirror.gcr.io/library/postgres:17").asCompatibleSubstituteFor("postgres"));
    private static final JdbcClient JDBC;

    static {
        POSTGRES.start();
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("idempotency/idempotency_record.sql")).execute(dataSource);
        JDBC = JdbcClient.create(dataSource);
    }

    private PostgresTestDatabase() {}

    static JdbcClient jdbc() {
        return JDBC;
    }
}
