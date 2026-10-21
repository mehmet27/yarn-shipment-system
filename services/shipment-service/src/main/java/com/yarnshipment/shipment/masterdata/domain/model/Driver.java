package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Şoför.
 *
 * @param id kimlik
 * @param fullName ad soyad
 * @param subcontractorId bağlı olduğu fasoncu
 */
public record Driver(UUID id, String fullName, @Nullable UUID subcontractorId) {

    public Driver {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fullName, "fullName");
    }
}
