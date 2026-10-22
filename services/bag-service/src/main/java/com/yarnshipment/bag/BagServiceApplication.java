package com.yarnshipment.bag;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** bag-service giriş noktası. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
public final class BagServiceApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }

    static void main(String[] args) {
        SpringApplication.run(BagServiceApplication.class, args);
    }
}
