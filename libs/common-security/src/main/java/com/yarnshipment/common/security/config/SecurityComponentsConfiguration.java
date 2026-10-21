package com.yarnshipment.common.security.config;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.audit.AuthorizationDeniedAuditListener;
import com.yarnshipment.common.security.audit.LogAuditPublisher;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.web.TokenRelayInterceptor;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;

/**
 * Servis kodunun kullandığı güvenlik bileşenleri ve yetki reddi audit'i. Servis kendi {@link AuditPublisher}
 * (outbox) veya {@link Clock} bean'ini tanımlarsa o kullanılır; yoksa log yayıncısı ve UTC saati devreye girer.
 */
@Configuration(proxyBeanMethods = false)
public final class SecurityComponentsConfiguration {

    @Bean
    AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringAuthorizationEventPublisher(publisher);
    }

    @Bean
    AuthorizationDeniedAuditListener authorizationDeniedAuditListener(
            ObjectProvider<AuditPublisher> auditPublisher, ObjectProvider<Clock> clock) {
        return new AuthorizationDeniedAuditListener(auditPublisherOf(auditPublisher), clockOf(clock));
    }

    @Bean
    ScopeGuard scopeGuard(ObjectProvider<AuditPublisher> auditPublisher, ObjectProvider<Clock> clock) {
        return new ScopeGuard(auditPublisherOf(auditPublisher), clockOf(clock));
    }

    @Bean
    CurrentActorProvider currentActorProvider() {
        return new CurrentActorProvider();
    }

    @Bean
    TokenRelayInterceptor tokenRelayInterceptor() {
        return new TokenRelayInterceptor();
    }

    private static AuditPublisher auditPublisherOf(ObjectProvider<AuditPublisher> provider) {
        return provider.getIfAvailable(LogAuditPublisher::new);
    }

    private static Clock clockOf(ObjectProvider<Clock> provider) {
        return provider.getIfAvailable(Clock::systemUTC);
    }
}
