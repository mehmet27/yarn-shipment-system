package com.yarnshipment.evidence;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

/** evidence-service giriş noktası. */
@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
public final class EvidenceServiceApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    static void main(String[] args) {
        SpringApplication.run(EvidenceServiceApplication.class, args);
    }
}
