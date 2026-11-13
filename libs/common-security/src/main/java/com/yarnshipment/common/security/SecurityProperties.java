package com.yarnshipment.common.security;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code yarnshipment.security.*} ayarları (CONF-01, CONF-02).
 *
 * @param publicPaths oturum istemeyen yollar
 * @param csrfEnabled durum değiştiren isteklerde {@code X-XSRF-TOKEN} başlığı istensin mi; yalnızca testlerde kapatılır
 */
@ConfigurationProperties("yarnshipment.security")
@Validated
public record SecurityProperties(
        @NotNull @DefaultValue({"/actuator/health/**", "/actuator/info", "/api/v1/auth/login"})
        List<String> publicPaths,

        @DefaultValue("true") boolean csrfEnabled) {

    public SecurityProperties {
        publicPaths = List.copyOf(publicPaths);
    }
}
