package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Araç.
 *
 * @param id kimlik
 * @param plateNo plaka
 * @param active listelerde görünür mü; pasif kayıt yalnızca yönetici sorgusunda gelir
 */
public record Vehicle(UUID id, String plateNo, boolean active) {

    /** Etkin kayıt. */
    public Vehicle(UUID id, String plateNo) {
        this(id, plateNo, true);
    }

    public Vehicle {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(plateNo, "plateNo");
    }
}
