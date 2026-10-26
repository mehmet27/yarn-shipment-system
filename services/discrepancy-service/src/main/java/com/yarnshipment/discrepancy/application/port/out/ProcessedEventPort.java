package com.yarnshipment.discrepancy.application.port.out;

import java.util.UUID;

/** İdempotent tüketici kaydı (EVT-05); işlemle aynı transaction'da yazılır. */
public interface ProcessedEventPort {

    /** Olay ilk kez işleniyorsa kaydeder ve {@code true} döner. */
    boolean markProcessed(UUID eventId, String eventType);
}
