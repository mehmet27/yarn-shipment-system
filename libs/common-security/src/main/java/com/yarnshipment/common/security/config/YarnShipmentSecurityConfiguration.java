package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.KeycloakJwtConverter;
import com.yarnshipment.common.security.SecurityProperties;
import com.yarnshipment.common.security.web.SecurityProblemResponses;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stateless resource server zinciri: her istek JWT ile doğrulanır (SEC-01), tanımlı açık yollar dışında her uç
 * kimlik ister ve yetki {@code @PreAuthorize} ile verilir (SEC-02). 401 ve 403 cevapları ERR-07 biçimindedir.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public final class YarnShipmentSecurityConfiguration {

    private static final String MESSAGES_BASENAME = "yarnshipment-security-messages";

    @Bean
    SecurityFilterChain yarnShipmentSecurityFilterChain(
            HttpSecurity http, SecurityProperties properties, SecurityProblemResponses problems) {
        String[] publicPaths = properties.publicPaths().toArray(String[]::new);
        // Bearer token ile çalışan stateless API'de çerez oturumu yoktur; CSRF koruması gateway'deki BFF'tedir.
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(publicPaths)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(server -> server.jwt(
                                jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtConverter(properties)))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(
                        handling -> handling.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        return http.build();
    }

    @Bean
    SecurityProblemResponses securityProblemResponses(ObjectProvider<JsonMapper> jsonMapper) {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename(MESSAGES_BASENAME);
        messages.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messages.setFallbackToSystemLocale(false);
        return new SecurityProblemResponses(messages, jsonMapper.getIfAvailable(JsonMapper::shared));
    }
}
