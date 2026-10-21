package com.yarnshipment.shipment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** {@code seal} tablosunun okuma eşlemesi. */
@Entity
@Immutable
@Table(name = "seal")
@SuppressWarnings("NullAway.Init")
class SealJpaEntity {

    private static final String TEXT = "text";

    @Id
    private UUID id;

    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "side", columnDefinition = TEXT)
    private String side;

    @Column(name = "seal_no", columnDefinition = TEXT)
    private String sealNo;

    @Column(name = "seal_condition", columnDefinition = TEXT)
    private String sealCondition;

    @Column(name = "superseded_by")
    private @Nullable UUID supersededBy;

    protected SealJpaEntity() {
        // JPA
    }

    UUID getShipmentId() {
        return shipmentId;
    }

    String getSide() {
        return side;
    }

    String getSealNo() {
        return sealNo;
    }

    String getSealCondition() {
        return sealCondition;
    }
}
