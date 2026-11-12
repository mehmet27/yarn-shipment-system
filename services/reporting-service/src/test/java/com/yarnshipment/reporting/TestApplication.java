package com.yarnshipment.reporting;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import com.yarnshipment.common.web.config.EnableYarnShipmentWeb;
import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** reporting modülünün dilim testleri için açılış sınıfı; üretimde tek uygulama (yarnshipment-app) açılır. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableYarnShipmentSecurity
@EnableYarnShipmentWeb
final class TestApplication {

    /** Zaman tek kaynaktan gelir; testler sabit saat verir (DOM-10). */
    @Bean
    static Clock clock() {
        return Clock.systemUTC();
    }
}
