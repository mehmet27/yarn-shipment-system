package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Fasoncu.
 *
 * @param id kimlik
 * @param title unvan
 */
public record Subcontractor(UUID id, String title) {

    public Subcontractor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
    }
}
