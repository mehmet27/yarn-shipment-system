package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.ShipmentParties;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Sevkiyat taraflarının anlık görüntü kolonları. */
@Embeddable
@SuppressWarnings("NullAway.Init")
final class PartiesEmbeddable {

    private static final String TEXT = "text";

    @Column(name = "subcontractor_id")
    private UUID subcontractorId;

    @Column(name = "subcontractor_title", columnDefinition = TEXT)
    private String subcontractorTitle;

    @Column(name = "driver_id")
    private @Nullable UUID driverId;

    @Column(name = "driver_name", columnDefinition = TEXT)
    private @Nullable String driverName;

    @Column(name = "plate_no", columnDefinition = TEXT)
    private @Nullable String plateNo;

    ShipmentParties toDomain() {
        return new ShipmentParties(subcontractorId, subcontractorTitle, driverId, driverName, plateNo);
    }
}
