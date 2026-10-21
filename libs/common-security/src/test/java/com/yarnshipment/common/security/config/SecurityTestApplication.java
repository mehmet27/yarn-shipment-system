package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Güvenlik zincirini gerçek Keycloak olmadan çalıştıran test uygulaması. */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableYarnShipmentSecurity
@Import(SecuredTestController.class)
class SecurityTestApplication {

    @Bean
    StubJwtDecoder jwtDecoder() {
        return new StubJwtDecoder();
    }

    @Bean
    RecordingAuditPublisher auditPublisher() {
        return new RecordingAuditPublisher();
    }

    /** Kayıtlı token değerini karşılığı olan JWT'ye çevirir; bilinmeyen token geçersizdir. */
    static final class StubJwtDecoder implements JwtDecoder {

        private final Map<String, Jwt> tokens = new ConcurrentHashMap<>();

        String register(Jwt jwt) {
            tokens.put(jwt.getTokenValue(), jwt);
            return jwt.getTokenValue();
        }

        @Override
        public Jwt decode(String token) {
            Jwt jwt = tokens.get(token);
            if (jwt == null) {
                throw new BadJwtException("Unknown test token");
            }
            return jwt;
        }
    }

    /** Yayınlanan audit olaylarını testte okumak için saklar. */
    static final class RecordingAuditPublisher implements AuditPublisher {

        private final List<SecurityAuditEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void publish(SecurityAuditEvent event) {
            events.add(event);
        }

        List<SecurityAuditEvent> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
