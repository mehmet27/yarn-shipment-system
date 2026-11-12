package com.yarnshipment.shipment;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import com.yarnshipment.common.web.platform.PlatformSchemaMigration;
import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

/** shipment modülünün dilim testleri için açılış sınıfı; üretimde tek uygulama (yarnshipment-app) açılır. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
final class TestApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    /** Modül göçünden önce ortak platform şeması kurulur (üretimde yarnshipment-app aynı sırayı uygular). */
    @Bean
    static FlywayMigrationStrategy platformFirst() {
        return flyway -> {
            PlatformSchemaMigration.migrate(flyway);
            flyway.migrate();
        };
    }
}
