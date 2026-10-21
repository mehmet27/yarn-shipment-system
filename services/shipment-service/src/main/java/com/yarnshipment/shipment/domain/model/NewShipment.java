package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Henüz kaydedilmemiş sevkiyat taslağı. Şoför ve araç birlikte ya da hiç verilir; ad ve plaka anlık görüntüdür.
 *
 * @param siteId çıkış lokasyonu
 * @param parties fasoncu, şoför ve plaka
 * @param vehicleId araç kimliği; plaka ile birlikte ya da hiç verilir
 * @param createdAt oluşturma zamanı
 */
public record NewShipment(
        UUID siteId, ShipmentParties parties, @Nullable UUID vehicleId, Instant createdAt) {

    public NewShipment {
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(parties, "parties");
        Objects.requireNonNull(createdAt, "createdAt");
        if ((parties.driverId() == null) != (parties.driverName() == null)) {
            throw new InvalidNewShipmentException("driverId", "driver id and name come together");
        }
        if ((vehicleId == null) != (parties.plateNo() == null)) {
            throw new InvalidNewShipmentException("vehicleId", "vehicle id and plate come together");
        }
    }
}
