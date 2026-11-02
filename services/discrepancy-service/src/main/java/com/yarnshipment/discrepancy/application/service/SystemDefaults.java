package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.domain.model.BusinessDays;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/** Olaydan ya da zamanlanmış kontrolden yapılan yazımların ortak değerleri. */
final class SystemDefaults {

    /** shipment-service'teki sistem hesabıyla aynı; geçmişte "Sistem" olarak görünür. */
    static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    /** İş günü hesabı fabrikanın saat dilimiyle yapılır. */
    private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

    private SystemDefaults() {}

    /** Uyuşmazlığın hedef kapanış zamanı: açılıştan 5 iş günü sonra (ön analiz, Bölüm 12.2). */
    static Instant dueAt(Instant openedAt) {
        return BusinessDays.plus(openedAt, BusinessDays.DISCREPANCY_DUE_DAYS, FACTORY_ZONE);
    }
}
