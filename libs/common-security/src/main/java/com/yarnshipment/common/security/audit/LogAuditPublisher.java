package com.yarnshipment.common.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Olayı ayrı {@code AUDIT} logger'ına yazar. Outbox tabanlı yayıncı gelene kadar geçici varsayılandır; IP ve cihaz
 * gibi istemci bilgileri log'a yazılmaz (SEC-12), izleme kimliği MDC'den gelir (LOG-02).
 */
public final class LogAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Override
    public void publish(SecurityAuditEvent event) {
        log.info(
                "Security decision recorded decision={} action={} resource={} actorUserId={} actorRoles={} reason={}",
                event.decision(),
                event.action(),
                event.resource(),
                event.actorUserId(),
                event.actorRoles(),
                event.reason());
    }
}
