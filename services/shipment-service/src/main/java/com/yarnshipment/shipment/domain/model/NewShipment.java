package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Henüz kaydedilmemiş sevkiyat taslağı. Şoför ve araç birlikte ya da hiç verilir; ad ve plaka anlık görüntüdür.
 *
 * @param siteId çıkış lokasyonu
 * @param parties fasoncu, şoför ve plaka
 * @param vehicleId araç kimliği; plaka ile birlikte ya da hiç verilir
 * @param lines reçete satırları, en az bir
 * @param fireRate fire oranı (0.04 = %4), 0 ile 0.20 arası
 * @param createdAt oluşturma zamanı
 */
public record NewShipment(
        UUID siteId,
        ShipmentParties parties,
        @Nullable UUID vehicleId,
        List<RecipeLine> lines,
        BigDecimal fireRate,
        Instant createdAt) {

    public NewShipment {
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(parties, "parties");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(fireRate, "fireRate");
        if (lines.isEmpty()) {
            throw new InvalidNewShipmentException("lines", "at least one recipe line");
        }
        lines = List.copyOf(lines);
        fireRate = ShipmentSettings.requireFireRate(fireRate, "fireRate");
        if ((parties.driverId() == null) != (parties.driverName() == null)) {
            throw new InvalidNewShipmentException("driverId", "driver id and name come together");
        }
        if ((vehicleId == null) != (parties.plateNo() == null)) {
            throw new InvalidNewShipmentException("vehicleId", "vehicle id and plate come together");
        }
    }

    /** Beklenen net kilo: reçete satırlarının toplamı. */
    public BigDecimal plannedNetKg() {
        return Recipe.totalKg(lines);
    }
}
