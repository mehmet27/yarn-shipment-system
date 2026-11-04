package com.yarnshipment.audit.adapter.out.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Bir PostgreSQL konteyneri. Flyway sahibi (migrator) rolle, yazma ve okuma uygulama rolleriyle ayrı bağlanır; böylece
 * migration'lardaki GRANT eksikleri testte yakalanır. Testler veri bıraktığı için her sınıf kendi konteynerini alır.
 */
final class PostgresTestDatabase {

    private static final String WRITER_ROLE = "audit_writer";
    private static final String READER_ROLE = "audit_reader";
    private static final String PASSWORD = "test";

    private final PostgreSQLContainer postgres;

    private PostgresTestDatabase(PostgreSQLContainer postgres) {
        this.postgres = postgres;
    }

    static PostgresTestDatabase start() {
        PostgreSQLContainer container = new PostgreSQLContainer(
                        DockerImageName.parse("mirror.gcr.io/library/postgres:17")
                                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("audit_db")
                .withUsername("audit_migrator")
                .withPassword(PASSWORD)
                .withInitScript("db/test-roles.sql");
        container.start();
        return new PostgresTestDatabase(container);
    }

    void registerIn(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> WRITER_ROLE);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("yarnshipment.audit.reader.url", postgres::getJdbcUrl);
        registry.add("yarnshipment.audit.reader.username", () -> READER_ROLE);
        registry.add("yarnshipment.audit.reader.password", () -> PASSWORD);
    }
}
