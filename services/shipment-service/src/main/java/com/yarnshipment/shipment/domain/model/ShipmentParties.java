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
 * @param sourceSubcontractorId iplik başka bir fasoncudan gidiyorsa gönderen fasoncu; boşsa merkezden
 * @param sourceSubcontractorTitle gönderen fasoncu unvanı
 */
public record ShipmentParties(
        UUID subcontractorId,
        String subcontractorTitle,
        @Nullable UUID driverId,
        @Nullable String driverName,
        @Nullable String plateNo,
        @Nullable UUID sourceSubcontractorId,
        @Nullable String sourceSubcontractorTitle) {

    public ShipmentParties {
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(subcontractorTitle, "subcontractorTitle");
        if ((sourceSubcontractorId == null) != (sourceSubcontractorTitle == null)) {
            throw new InvalidNewShipmentException("sourceSubcontractorId", "source id and title come together");
        }
        if (subcontractorId.equals(sourceSubcontractorId)) {
            throw new InvalidNewShipmentException("sourceSubcontractorId", "source and destination must differ");
        }
    }

    /** Merkezden çıkan sevkiyat: gönderen fasoncu yok. */
    public ShipmentParties(
            UUID subcontractorId,
            String subcontractorTitle,
            @Nullable UUID driverId,
            @Nullable String driverName,
            @Nullable String plateNo) {
        this(subcontractorId, subcontractorTitle, driverId, driverName, plateNo, null, null);
    }

    /** SEC-13: şoför adı kişisel veridir. */
    @Override
    public String toString() {
        return "ShipmentParties[subcontractorId=" + subcontractorId + ", driverId=" + driverId + ", driverName=***]";
    }
}
