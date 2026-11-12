package com.yarnshipment.app;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Bir PostgreSQL konteyneri; testler boyunca bir kez başlar. Docker olmayan makinede
 * {@code YARNSHIPMENT_TEST_DB_URL} (boş bir veritabanı, kullanıcı {@code yarnshipment}, şifre {@code test}) verilirse
 * konteyner yerine o veritabanı kullanılır.
 */
final class PostgresTestDatabase {

    private static final String USER = "yarnshipment";
    private static final String PASSWORD = "test";
    private static final String EXTERNAL_URL =
            Objects.requireNonNullElse(System.getenv("YARNSHIPMENT_TEST_DB_URL"), "");
    private static final @Nullable PostgreSQLContainer CONTAINER = EXTERNAL_URL.isEmpty() ? start() : null;

    private PostgresTestDatabase() {}

    private static PostgreSQLContainer start() {
        PostgreSQLContainer container = new PostgreSQLContainer(
                        DockerImageName.parse("mirror.gcr.io/library/postgres:17")
                                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("yarnshipment")
                .withUsername(USER)
                .withPassword(PASSWORD);
        container.start();
        return container;
    }

    private static String jdbcUrl() {
        return CONTAINER == null ? EXTERNAL_URL : CONTAINER.getJdbcUrl();
    }

    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestDatabase::jdbcUrl);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }
}
