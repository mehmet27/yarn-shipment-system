package com.yarnshipment.app;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * yarnshipment-app giriş noktası: shipment, discrepancy, evidence, reporting ve audit modülleri tek süreçte açılır.
 *
 * <p>Modüller ayrı Maven modülleridir ve her biri kendi paketinde yaşar; bean adları paket adıyla birlikte üretilir,
 * çünkü modüller aynı sınıf adını (ör. {@code UuidV7Generator}) kendi içlerinde tutabilir. Varlık ve repository
 * taraması {@code com.yarnshipment} altındadır (yalnız shipment modülünde JPA varlığı vardır).
 */
@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackages = {
            "com.yarnshipment.app",
            "com.yarnshipment.shipment",
            "com.yarnshipment.discrepancy",
            "com.yarnshipment.evidence",
            "com.yarnshipment.reporting",
            "com.yarnshipment.audit"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@AutoConfigurationPackage(basePackages = "com.yarnshipment")
@ConfigurationPropertiesScan("com.yarnshipment")
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
@EnableScheduling
public final class YarnShipmentApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(YarnShipmentApplication.class, args);
    }
}
