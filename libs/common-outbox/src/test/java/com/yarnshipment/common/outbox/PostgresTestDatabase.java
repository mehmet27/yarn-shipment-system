package com.yarnshipment.common.outbox;

import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Testler boyunca bir kez başlatılan PostgreSQL; outbox ve processed_event tablolarıyla hazırdır. */
final class PostgresTestDatabase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("mirror.gcr.io/library/postgres:17").asCompatibleSubstituteFor("postgres"));
    private static final DataSource DATA_SOURCE;

    static {
        POSTGRES.start();
        DATA_SOURCE =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("outbox-test-schema.sql")).execute(DATA_SOURCE);
    }

    private PostgresTestDatabase() {}

    static DataSource dataSource() {
        return DATA_SOURCE;
    }
}
