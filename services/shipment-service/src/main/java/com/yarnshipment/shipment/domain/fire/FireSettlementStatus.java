package com.yarnshipment.shipment.domain.fire;

/** Fire dosyasının durumu (analiz 5.11). */
public enum FireSettlementStatus {
    /** Sevkiyat tamamlandı, henüz kumaş girişi yok. */
    AWAITING_FABRIC,
    /** Kumaş girmeye başladı; müdür fatura edecek ya da kapatacak. */
    TO_INVOICE,
    /** Hatalı kg faturalandı; geri alınamaz. */
    INVOICED,
    /** Hatalı kg sıfırdı, müdür kapattı. */
    CLOSED;

    /** Kapanmış dosyaya kumaş girişi, iplik dönüşü ve oran değişikliği yapılamaz. */
    public boolean isFinal() {
        return this == INVOICED || this == CLOSED;
    }
}
