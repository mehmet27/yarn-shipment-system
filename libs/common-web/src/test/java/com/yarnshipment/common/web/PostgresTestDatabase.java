package com.yarnshipment.common.web;

import com.yarnshipment.common.web.platform.PlatformSchemaMigration;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Testler boyunca bir kez başlatılan PostgreSQL; {@code platform} şemasıyla hazırdır. */
final class PostgresTestDatabase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("mirror.gcr.io/library/postgres:17").asCompatibleSubstituteFor("postgres"));
    private static final DriverManagerDataSource DATA_SOURCE;
    private static final JdbcClient JDBC;

    static {
        POSTGRES.start();
        DATA_SOURCE =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        PlatformSchemaMigration.migrate(DATA_SOURCE, Map.of("app_role", POSTGRES.getUsername()));
        JDBC = JdbcClient.create(DATA_SOURCE);
    }

    private PostgresTestDatabase() {}

    static JdbcClient jdbc() {
        return JDBC;
    }

    /** Transaction açan testler için; {@link #jdbc()} aynı kaynağa bağlıdır ve açık transaction'a katılır. */
    static DataSource dataSource() {
        return DATA_SOURCE;
    }
}
