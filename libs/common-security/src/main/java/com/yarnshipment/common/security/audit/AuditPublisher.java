package com.yarnshipment.common.security.audit;

/**
 * Güvenlik audit olaylarının gideceği yer. Varsayılan {@link LogAuditPublisher}'dır; servis outbox'a yazan kendi
 * uygulamasını bean olarak tanımladığında o kullanılır (LOG-06).
 */
public interface AuditPublisher {

    /** Olayı yayınlar; çağıranın işlemini bozmamak için istisna fırlatmaz. */
    void publish(SecurityAuditEvent event);
}
