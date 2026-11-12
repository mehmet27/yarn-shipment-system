package com.yarnshipment.app;

import com.yarnshipment.common.web.platform.PlatformSchemaMigration;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Her modülün şeması kendi Flyway'iyle ve kendi {@code flyway_schema_history} tablosuyla göç eder; mevcut
 * veritabanlarında geçmiş, checksum ve sürümler değişmez. Modüllerin migration dosyaları
 * {@code db/migration/<şema>} altındadır.
 *
 * <p>Önce ortak {@code platform} şeması göç eder (Idempotency-Key kayıtları ve {@code audit_local}; kendi geçmişi
 * var): modül migration'ları trigger'larını platform fonksiyonuna bağlar. Sıra önemlidir: reporting görünümleri
 * shipment ve discrepancy tablolarını okur, bu yüzden reporting en sona kalır. Spring Boot'un tek Flyway'i bu
 * yapılandırma yüzünden devreye girmez; göç, JDBC ve JPA bean'leri başlamadan biter
 * ({@link FlywayMigrationInitializer} veritabanı başlatıcısı olarak tanınır).
 */
@Configuration(proxyBeanMethods = false)
class DatabaseMigrationConfiguration {

    static final String SHIPMENT = "shipment";
    static final String DISCREPANCY = "discrepancy";
    static final String EVIDENCE = "evidence";
    static final String AUDIT = "audit";
    static final String REPORTING = "reporting";

    private static final String MIGRATION_ROOT = "classpath:db/migration/";
    private static final String SEED_ROOT = "classpath:db/seed-local/";

    private final DataSource dataSource;
    private final String databaseUser;
    private final boolean seedLocal;

    DatabaseMigrationConfiguration(
            DataSource dataSource,
            @Value("${spring.datasource.username}") String databaseUser,
            @Value("${yarnshipment.database.seed-local:false}") boolean seedLocal) {
        this.dataSource = dataSource;
        this.databaseUser = databaseUser;
        this.seedLocal = seedLocal;
    }

    @Bean
    InitializingBean platformMigration() {
        return () -> PlatformSchemaMigration.migrate(dataSource, Map.of("app_role", databaseUser));
    }

    @Bean
    Flyway shipmentFlyway() {
        return flyway(SHIPMENT, true);
    }

    @Bean
    Flyway discrepancyFlyway() {
        return flyway(DISCREPANCY, true);
    }

    @Bean
    Flyway evidenceFlyway() {
        return flyway(EVIDENCE, false);
    }

    @Bean
    Flyway auditFlyway() {
        return flyway(AUDIT, false);
    }

    @Bean
    Flyway reportingFlyway() {
        return flyway(REPORTING, false);
    }

    @Bean
    @DependsOn("platformMigration")
    FlywayMigrationInitializer shipmentMigration(@Qualifier("shipmentFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    @DependsOn("shipmentMigration")
    FlywayMigrationInitializer discrepancyMigration(@Qualifier("discrepancyFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    @DependsOn("discrepancyMigration")
    FlywayMigrationInitializer evidenceMigration(@Qualifier("evidenceFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    @DependsOn("evidenceMigration")
    FlywayMigrationInitializer auditMigration(@Qualifier("auditFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    @DependsOn("auditMigration")
    FlywayMigrationInitializer reportingMigration(@Qualifier("reportingFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    /**
     * @param schema modülün şeması; migration konumu ve geçmiş tablosunun yeri
     * @param hasLocalSeed yerel örnek veri dosyası olan modül; yalnız {@code yarnshipment.database.seed-local} açıkken
     */
    private Flyway flyway(String schema, boolean hasLocalSeed) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .placeholders(Map.of("app_role", databaseUser, "readonly_role", databaseUser));
        if (seedLocal && hasLocalSeed) {
            configuration.locations(MIGRATION_ROOT + schema, SEED_ROOT + schema);
        } else {
            configuration.locations(MIGRATION_ROOT + schema);
        }
        return configuration.load();
    }
}
