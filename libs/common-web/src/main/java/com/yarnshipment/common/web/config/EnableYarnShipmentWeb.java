package com.yarnshipment.common.web.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Servisin ana sınıfına eklenir; ortak hata işleyiciyi ve istek kimliğini açar. Idempotency-Key, serviste
 * {@code JdbcClient} varsa kendiliğinden devreye girer (bkz. {@link IdempotencyAutoConfiguration}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(WebComponentsConfiguration.class)
public @interface EnableYarnShipmentWeb {}
