package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Araç.
 *
 * @param id kimlik
 * @param plateNo plaka
 */
public record Vehicle(UUID id, String plateNo) {

    public Vehicle {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(plateNo, "plateNo");
    }
}
