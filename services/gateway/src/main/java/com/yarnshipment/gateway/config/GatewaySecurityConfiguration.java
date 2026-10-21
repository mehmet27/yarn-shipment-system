package com.yarnshipment.gateway.config;

import com.yarnshipment.gateway.web.BrowserEntryPoint;
import com.yarnshipment.gateway.web.CsrfCookieFilter;
import com.yarnshipment.gateway.web.JsonLogoutSuccessHandler;
import com.yarnshipment.gateway.web.ReturnToFilter;
import com.yarnshipment.gateway.web.ReturnToSuccessHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * BFF zinciri: tarayıcı çerez oturumuyla gelir, giriş Keycloak'ta yapılır (SEC-01). Token tarayıcıya inmez; servislere
 * route filtresi iletir. Durum değiştiren her istek {@code X-XSRF-TOKEN} başlığı ister.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class GatewaySecurityConfiguration {

    private static final String LOGIN_PATH = "/oauth2/authorization/keycloak";

    @Bean
    SecurityFilterChain gatewaySecurityFilterChain(
            HttpSecurity http,
            GatewayProperties properties,
            ClientRegistrationRepository registrations,
            JsonMapper jsonMapper)
            throws Exception {
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        JsonLogoutSuccessHandler logout =
                new JsonLogoutSuccessHandler(registrations, properties.postLogoutUrl(), jsonMapper);
        http.authorizeHttpRequests(requests -> requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(login -> login.successHandler(new ReturnToSuccessHandler(properties.postLoginUrl())))
                .logout(configurer -> configurer.logoutSuccessHandler(logout))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(csrfTokens), CsrfFilter.class)
                .addFilterBefore(
                        new ReturnToFilter(properties.postLoginUrl()), OAuth2AuthorizationRequestRedirectFilter.class)
                .exceptionHandling(
                        handling -> handling.authenticationEntryPoint(new BrowserEntryPoint(LOGIN_PATH, jsonMapper)));
        return http.build();
    }
}
