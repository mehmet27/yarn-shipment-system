package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Fasoncu.
 *
 * @param id kimlik
 * @param title unvan
 * @param active listelerde görünür mü; pasif kayıt yalnızca yönetici sorgusunda gelir
 */
public record Subcontractor(UUID id, String title, boolean active) {

    /** Etkin kayıt. */
    public Subcontractor(UUID id, String title) {
        this(id, title, true);
    }

    public Subcontractor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
    }
}
