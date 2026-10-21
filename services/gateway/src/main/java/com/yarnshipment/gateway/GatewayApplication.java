package com.yarnshipment.gateway;

import com.yarnshipment.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** gateway giriş noktası. */
@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProperties.class)
public final class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
