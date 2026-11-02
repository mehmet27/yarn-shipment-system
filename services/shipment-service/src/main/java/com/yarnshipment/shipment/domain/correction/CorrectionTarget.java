package com.yarnshipment.shipment.domain.correction;

import com.yarnshipment.shipment.domain.correction.CorrectedValue.SealNo;
import com.yarnshipment.shipment.domain.correction.CorrectedValue.Weight;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentSide;
import java.util.Optional;

/**
 * Düzeltilebilen kayıt. Tartım ve mühür hiç güncellenmez; düzeltme yeni satır ekler, eskisi {@code superseded_by} ile
 * saklanır (DOM-16).
 */
public enum CorrectionTarget {
    DISPATCH_WEIGHING("WEIGHING", "DISPATCH"),
    DELIVERY_WEIGHING("WEIGHING", "DELIVERY"),
    DISPATCH_SEAL_NO("SEAL", "DISPATCH"),
    DELIVERY_SEAL_NO("SEAL", "DELIVERY");

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

    /** Tartım ve mühür tablolarındaki {@code side} değeri. */
    public String side() {
        return side;
    }

    public boolean isWeighing() {
        return "WEIGHING".equals(entityType);
    }

    /** Sevkiyattaki geçerli değer; kayıt hiç girilmediyse boş. */
    public Optional<CorrectedValue> currentValue(Shipment shipment) {
        ShipmentSide data = "DISPATCH".equals(side) ? shipment.dispatch() : shipment.delivery();
        if (isWeighing()) {
            return Optional.ofNullable(data.weighing()).map(w -> new Weight(w.grossKg(), w.tareKg()));
        }
        return Optional.ofNullable(data.sealNo()).map(SealNo::new);
    }
}
