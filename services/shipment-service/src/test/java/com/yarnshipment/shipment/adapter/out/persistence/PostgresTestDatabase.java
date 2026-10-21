package com.yarnshipment.shipment.adapter.out.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Bir PostgreSQL konteyneri. Flyway sahibi (migrator) rolle, uygulama DML rolüyle bağlanır; böylece migration'lardaki
 * GRANT eksikleri testte yakalanır. Veri bırakan testler için {@link #startIsolated()} ayrı bir konteyner verir.
 */
public final class PostgresTestDatabase {

    private static final String APP_ROLE = "shipment_app";
    private static final String APP_PASSWORD = "test";
    /** Testler boyunca bir kez başlatılır; bu veritabanına yazan testler kendi kaydını geri alır. */
    private static final PostgresTestDatabase SHARED = startIsolated();

    private final PostgreSQLContainer postgres;

    private PostgresTestDatabase(PostgreSQLContainer postgres) {
        this.postgres = postgres;
    }

    /** Yalnızca çağıran testin kullandığı, commit edilen verinin başka testleri etkilemediği yeni konteyner. */
    public static PostgresTestDatabase startIsolated() {
        PostgreSQLContainer container = new PostgreSQLContainer(
                        DockerImageName.parse("mirror.gcr.io/library/postgres:17")
                                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("shipment_db")
                .withUsername("shipment_migrator")
                .withPassword(APP_PASSWORD)
                .withInitScript("db/test-roles.sql");
        container.start();
        return new PostgresTestDatabase(container);
    }

    public static void register(DynamicPropertyRegistry registry) {
        SHARED.registerIn(registry);
    }

    public void registerIn(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_ROLE);
        registry.add("spring.datasource.password", () -> APP_PASSWORD);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
}
