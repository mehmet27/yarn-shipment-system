package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Lokasyon (fabrika veya depo).
 *
 * @param id kimlik
 * @param name ad
 */
public record Site(UUID id, String name) {

    public Site {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
