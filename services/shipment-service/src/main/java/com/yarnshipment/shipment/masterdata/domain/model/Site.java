package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Lokasyon (fabrika veya depo).
 *
 * @param id kimlik
 * @param name ad
 * @param active listelerde görünür mü; pasif kayıt yalnızca yönetici sorgusunda gelir
 */
public record Site(UUID id, String name, boolean active) {

    /** Etkin kayıt. */
    public Site(UUID id, String name) {
        this(id, name, true);
    }

    public Site {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
