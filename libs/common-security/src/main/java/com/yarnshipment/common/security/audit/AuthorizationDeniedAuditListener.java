package com.yarnshipment.common.security.audit;

import com.yarnshipment.common.security.ActorAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;

/** {@code @PreAuthorize} ve URL kurallarının reddettiği istekleri audit'e yazar (SEC-02, SEC-03). */
public final class AuthorizationDeniedAuditListener {

    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public AuthorizationDeniedAuditListener(AuditPublisher auditPublisher, Clock clock) {
        this.auditPublisher = auditPublisher;
        this.clock = clock;
    }

    /** Spring Security'nin yetki reddi olayı. */
    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        Authentication authentication = event.getAuthentication().get();
        if (authentication instanceof ActorAuthenticationToken token) {
            String action = describe(event.getSource());
            auditPublisher.publish(SecurityAuditEvent.denied(clock.instant(), token.actor(), action, action)
                    .withReason("MISSING_PERMISSION"));
        }
    }

    private static String describe(Object source) {
        return switch (source) {
            case HttpServletRequest request -> request.getMethod() + " " + request.getRequestURI();
            case MethodInvocation invocation ->
                invocation.getMethod().getDeclaringClass().getSimpleName() + "."
                        + invocation.getMethod().getName();
            default -> source.getClass().getSimpleName();
        };
    }
}
