package com.yarnshipment.shipment.domain.correction;

import com.yarnshipment.shipment.domain.correction.CorrectedValue.Weight;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.Optional;

/**
 * Düzeltilebilen kayıt. Tartım hiç güncellenmez; düzeltme yeni satır ekler, eskisi {@code superseded_by} ile saklanır
 * (DOM-16).
 */
public enum CorrectionTarget {
    DISPATCH_WEIGHING("WEIGHING", "DISPATCH");

    private final String entityType;
    private final String side;

    CorrectionTarget(String entityType, String side) {
        this.entityType = entityType;
        this.side = side;
    }

    /** {@code correction_record.entity_type} değeri. */
    public String entityType() {
        return entityType;
    }

    /** Tartım tablosundaki {@code side} değeri. */
    public String side() {
        return side;
    }

    /** Sevkiyattaki geçerli değer; kayıt hiç girilmediyse boş. */
    public Optional<CorrectedValue> currentValue(Shipment shipment) {
        return Optional.ofNullable(shipment.dispatchWeighing()).map(w -> new Weight(w.grossKg(), w.tareKg()));
    }
}
