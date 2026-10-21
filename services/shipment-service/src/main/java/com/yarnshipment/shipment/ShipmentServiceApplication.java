package com.yarnshipment.shipment;

import com.yarnshipment.common.security.config.EnableYarnShipmentSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** shipment-service giriş noktası. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableYarnShipmentSecurity
public final class ShipmentServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ShipmentServiceApplication.class, args);
    }
}
