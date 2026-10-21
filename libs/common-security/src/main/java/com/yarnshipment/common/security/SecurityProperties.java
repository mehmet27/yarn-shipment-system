package com.yarnshipment.common.security;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code yarnshipment.security.*} ayarları (CONF-01, CONF-02). Issuer ve audience Spring Boot'un kendi ayarlarıyla
 * verilir: {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} ve {@code ...jwt.audiences}.
 *
 * @param mfaRequiredRoles yalnızca MFA ile açılmış oturumda geçerli roller (karar Y5)
 * @param mfaAmrValues {@code amr} claim'inde MFA sayılan değerler
 * @param mfaAcrValues {@code acr} claim'inde MFA sayılan değerler
 * @param publicPaths token istemeyen yollar
 */
@ConfigurationProperties("yarnshipment.security")
@Validated
public record SecurityProperties(
        @NotNull @DefaultValue({"ADMIN", "QUALITY"}) Set<Role> mfaRequiredRoles,
        @NotNull @DefaultValue({"otp", "mfa"}) Set<String> mfaAmrValues,
        @NotNull @DefaultValue("2") Set<String> mfaAcrValues,

        @NotNull @DefaultValue({"/actuator/health/**", "/actuator/info"})
        List<String> publicPaths) {

    public SecurityProperties {
        mfaRequiredRoles = Set.copyOf(mfaRequiredRoles);
        mfaAmrValues = Set.copyOf(mfaAmrValues);
        mfaAcrValues = Set.copyOf(mfaAcrValues);
        publicPaths = List.copyOf(publicPaths);
    }
}
