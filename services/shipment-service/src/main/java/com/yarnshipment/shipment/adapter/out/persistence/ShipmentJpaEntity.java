package com.yarnshipment.shipment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** {@code shipment} tablosunun okuma eşlemesi. Yazma adımları eklenene kadar değiştirilemez. */
@Entity
@Immutable
@Table(name = "shipment")
@SuppressWarnings("NullAway.Init")
class ShipmentJpaEntity {

    private static final String TEXT = "text";

    @Id
    private UUID id;

    @Column(name = "shipment_no", columnDefinition = TEXT)
    private String shipmentNo;

    @Column(name = "site_id")
    private UUID siteId;

    @Embedded
    private PartiesEmbeddable parties;

    @Column(name = "status", columnDefinition = TEXT)
    private String status;

    @Column(name = "planned_net_kg")
    private @Nullable BigDecimal plannedNetKg;

    @Column(name = "planned_bag_count")
    private @Nullable Integer plannedBagCount;

    @Column(name = "random_check_selected")
    private boolean randomCheckSelected;

    @Column(name = "created_at")
    private Instant createdAt;

    protected ShipmentJpaEntity() {
        // JPA
    }

    UUID getId() {
        return id;
    }

    String getShipmentNo() {
        return shipmentNo;
    }

    UUID getSiteId() {
        return siteId;
    }

    PartiesEmbeddable getParties() {
        return parties;
    }

    String getStatus() {
        return status;
    }

    @Nullable
    BigDecimal getPlannedNetKg() {
        return plannedNetKg;
    }

    @Nullable
    Integer getPlannedBagCount() {
        return plannedBagCount;
    }

    boolean isRandomCheckSelected() {
        return randomCheckSelected;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
