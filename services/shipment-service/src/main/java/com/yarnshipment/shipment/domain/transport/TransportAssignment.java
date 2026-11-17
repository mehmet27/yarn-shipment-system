package com.yarnshipment.shipment.domain.transport;

import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın yeni şoför ve araç bilgisi; ad ve plaka atama anındaki anlık görüntüdür. Şoför kimliği ile adı, araç
 * kimliği ile plakası birlikte verilir. Verilmeyen bilgi sevkiyatta olduğu gibi kalır; en az biri verilmelidir.
 *
 * @param driverId yeni şoför; boşsa şoför değişmez
 * @param driverName şoför adı
 * @param vehicleId yeni araç; boşsa araç değişmez
 * @param plateNo araç plakası
 */
public record TransportAssignment(
        @Nullable UUID driverId,
        @Nullable String driverName,
        @Nullable UUID vehicleId,
        @Nullable String plateNo) {

    public TransportAssignment {
        if ((driverId == null) != (driverName == null)) {
            throw new InvalidNewShipmentException("driverId", "driver id and name come together");
        }
        if ((vehicleId == null) != (plateNo == null)) {
            throw new InvalidNewShipmentException("vehicleId", "vehicle id and plate come together");
        }
        if (driverId == null && vehicleId == null) {
            throw new InvalidNewShipmentException("driverId", "driver or vehicle required");
        }
    }

    /** SEC-13: şoför adı kişisel veridir. */
    @Override
    public String toString() {
        return "TransportAssignment[driverId=" + driverId + ", driverName=***, vehicleId=" + vehicleId + "]";
    }
}
