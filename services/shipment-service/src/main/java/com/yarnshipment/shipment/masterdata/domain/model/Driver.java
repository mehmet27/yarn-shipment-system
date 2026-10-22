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
 * @param active listelerde görünür mü; pasif kayıt yalnızca yönetici sorgusunda gelir
 */
public record Driver(UUID id, String fullName, @Nullable UUID subcontractorId, boolean active) {

    /** Etkin kayıt. */
    public Driver(UUID id, String fullName, @Nullable UUID subcontractorId) {
        this(id, fullName, subcontractorId, true);
    }

    public Driver {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fullName, "fullName");
    }
}
