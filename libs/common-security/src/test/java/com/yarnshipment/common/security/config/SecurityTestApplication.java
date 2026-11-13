package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Güvenlik zincirini deneyen test uygulaması. */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableYarnShipmentSecurity
@Import({SecuredTestController.class, SignedUrlTestController.class})
class SecurityTestApplication {

    @Bean
    RecordingAuditPublisher auditPublisher() {
        return new RecordingAuditPublisher();
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
