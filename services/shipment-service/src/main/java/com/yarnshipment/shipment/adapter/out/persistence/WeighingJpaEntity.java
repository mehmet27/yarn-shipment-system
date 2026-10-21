package com.yarnshipment.shipment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** {@code weighing} tablosunun okuma eşlemesi. Net kilo veritabanında üretilir, burada okunmaz. */
@Entity
@Immutable
@Table(name = "weighing")
@SuppressWarnings("NullAway.Init")
class WeighingJpaEntity {

    @Id
    private UUID id;

    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "side", columnDefinition = "text")
    private String side;

    @Column(name = "gross_kg")
    private BigDecimal grossKg;

    @Column(name = "tare_kg")
    private BigDecimal tareKg;

    @Column(name = "bag_count")
    private int bagCount;

    @Column(name = "weighed_at")
    private Instant weighedAt;

    @Column(name = "superseded_by")
    private @Nullable UUID supersededBy;

    protected WeighingJpaEntity() {
        // JPA
    }

    UUID getShipmentId() {
        return shipmentId;
    }

    String getSide() {
        return side;
    }

    BigDecimal getGrossKg() {
        return grossKg;
    }

    BigDecimal getTareKg() {
        return tareKg;
    }

    int getBagCount() {
        return bagCount;
    }

    Instant getWeighedAt() {
        return weighedAt;
    }
}
