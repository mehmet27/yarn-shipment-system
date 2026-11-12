package com.yarnshipment.reporting;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** reporting-service giriş noktası. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
public final class ReportingServiceApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
