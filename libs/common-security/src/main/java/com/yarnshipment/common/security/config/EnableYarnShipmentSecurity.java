package com.yarnshipment.common.security.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Servisin ana sınıfına eklenir; ortak güvenlik zincirini (SEC-01…04) açar. Açık bir işaret olması, kütüphaneyi
 * yalnızca test için ekleyen modülün yanlışlıkla güvenlik zinciri almasını engeller.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({YarnShipmentSecurityConfiguration.class, SecurityComponentsConfiguration.class})
public @interface EnableYarnShipmentSecurity {}
