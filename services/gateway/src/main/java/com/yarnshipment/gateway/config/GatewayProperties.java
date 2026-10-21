package com.yarnshipment.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway'in tarayıcıyla ilgili ayarları.
 *
 * @param postLoginUrl giriş tamamlanınca tarayıcının döneceği adres (SPA kökü)
 * @param postLogoutUrl Keycloak oturumu kapanınca tarayıcının döneceği adres; Keycloak'ta kayıtlı olmalıdır
 */
@ConfigurationProperties(prefix = "yarnshipment.gateway")
public record GatewayProperties(String postLoginUrl, String postLogoutUrl) {}
