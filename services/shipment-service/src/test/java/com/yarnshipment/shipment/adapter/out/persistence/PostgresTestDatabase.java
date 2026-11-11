package com.yarnshipment.shipment.adapter.out.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Bir PostgreSQL konteyneri. Flyway sahibi (migrator) rolle, uygulama DML rolüyle bağlanır; böylece migration'lardaki
 * GRANT eksikleri testte yakalanır. Veri bırakan testler için {@link #startIsolated()} ayrı bir konteyner verir.
 */
public final class PostgresTestDatabase {

    private static final String APP_ROLE = "yarnshipment";
    private static final String APP_PASSWORD = "test";
    /** Testler boyunca bir kez başlatılır; bu veritabanına yazan testler kendi kaydını geri alır. */
    private static final PostgresTestDatabase SHARED = startIsolated();

    private final PostgreSQLContainer postgres;

    private PostgresTestDatabase(PostgreSQLContainer postgres) {
        this.postgres = postgres;
    }

    /**
     * Yalnızca çağıran testin kullandığı, commit edilen verinin başka testleri etkilemediği yeni konteyner.
     * Önbellekteki her Spring bağlamı kendi havuzunu açtığından bağlantı sınırı yükseltilir.
     */
    public static PostgresTestDatabase startIsolated() {
        PostgreSQLContainer container = new PostgreSQLContainer(
                        DockerImageName.parse("mirror.gcr.io/library/postgres:17")
                                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("yarnshipment")
                .withUsername("yarnshipment")
                .withPassword(APP_PASSWORD)
                .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=400");
        container.start();
        return new PostgresTestDatabase(container);
    }

    /** Şema sahibi rolle bağlanan şablon; uygulama rolünün yazamadığı tablolarda test verisi hazırlamak içindir. */
    public org.springframework.jdbc.core.JdbcTemplate migratorJdbc() {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        dataSource.setSchema("shipment");
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
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
