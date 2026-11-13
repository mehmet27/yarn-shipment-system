package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.CsrfCookieFilter;
import com.yarnshipment.common.security.SecurityProperties;
import com.yarnshipment.common.security.web.SecurityProblemResponses;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Çerez oturumlu güvenlik zinciri: giriş uygulamanın kendi ucundadır ({@code /api/v1/auth/login}); oturum sunucuda
 * tutulur, tarayıcıya yalnızca HttpOnly oturum çerezi iner (SEC-01). Tanımlı açık yollar dışında her uç oturum ister,
 * yetki {@code @PreAuthorize} ile verilir (SEC-02). Durum değiştiren isteklerde {@code X-XSRF-TOKEN} başlığı istenir.
 * 401 ve 403 cevapları ERR-07 biçimindedir.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
// Controller'lar OpenAPI'den üretilen arayüzleri uygular; JDK proxy'si @RestController'ı gizleyeceği için CGLIB.
@EnableMethodSecurity(proxyTargetClass = true)
@EnableConfigurationProperties(SecurityProperties.class)
@Import(SessionConfiguration.class)
public final class YarnShipmentSecurityConfiguration {

    private static final String MESSAGES_BASENAME = "yarnshipment-security-messages";
    /** Giriş çağrısı CSRF çerezi olmadan gelir; JSON gövdesi ve SameSite çerez giriş sahteciliğini engeller. */
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    @Bean
    SecurityFilterChain yarnShipmentSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            SecurityProblemResponses problems,
            SessionRegistry sessionRegistry) {
        String[] publicPaths = properties.publicPaths().toArray(String[]::new);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                .authorizeHttpRequests(requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(publicPaths)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(
                        handling -> handling.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        if (properties.csrfEnabled()) {
            CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
            http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .ignoringRequestMatchers(csrfIgnoredPaths(properties)))
                    .addFilterAfter(new CsrfCookieFilter(csrfTokens), CsrfFilter.class);
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }

    private static String[] csrfIgnoredPaths(SecurityProperties properties) {
        return Stream.concat(Stream.of(LOGIN_PATH), properties.csrfIgnoredPaths().stream())
                .toArray(String[]::new);
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
