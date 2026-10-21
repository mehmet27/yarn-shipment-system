package com.yarnshipment.shipment.adapter.out.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testler boyunca bir kez başlatılan PostgreSQL. Flyway sahibi (migrator) rolle, uygulama DML rolüyle bağlanır;
 * böylece migration'lardaki GRANT eksikleri testte yakalanır.
 */
final class PostgresTestDatabase {

    private static final String APP_ROLE = "shipment_app";
    private static final String APP_PASSWORD = "test";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
                    DockerImageName.parse("mirror.gcr.io/library/postgres:17").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("shipment_db")
            .withUsername("shipment_migrator")
            .withPassword(APP_PASSWORD)
            .withInitScript("db/test-roles.sql");

    static {
        POSTGRES.start();
    }

    private PostgresTestDatabase() {}

    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_ROLE);
        registry.add("spring.datasource.password", () -> APP_PASSWORD);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
