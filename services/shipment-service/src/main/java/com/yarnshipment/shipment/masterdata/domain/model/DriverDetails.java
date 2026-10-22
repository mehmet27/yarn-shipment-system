package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Şoförün düzenlenebilir alanları; {@code subcontractorId} boşsa şoför firmanın kendi şoförüdür. */
public record DriverDetails(String fullName, @Nullable UUID subcontractorId) {

    public DriverDetails {
        fullName = MasterDataText.clean("fullName", fullName);
    }
}
