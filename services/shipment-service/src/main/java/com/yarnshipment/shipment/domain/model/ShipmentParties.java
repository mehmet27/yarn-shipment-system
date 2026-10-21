package com.yarnshipment.shipment.domain.model;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın tarafları; ad ve plaka oluşturma anındaki anlık görüntüdür.
 *
 * @param subcontractorId fasoncu
 * @param subcontractorTitle fasoncu unvanı
 * @param driverId atanmış şoför
 * @param driverName şoför adı
 * @param plateNo araç plakası
 */
public record ShipmentParties(
        UUID subcontractorId,
        String subcontractorTitle,
        @Nullable UUID driverId,
        @Nullable String driverName,
        @Nullable String plateNo) {

    public ShipmentParties {
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(subcontractorTitle, "subcontractorTitle");
    }

    /** SEC-13: şoför adı kişisel veridir. */
    @Override
    public String toString() {
        return "ShipmentParties[subcontractorId=" + subcontractorId + ", driverId=" + driverId + ", driverName=***]";
    }
}
