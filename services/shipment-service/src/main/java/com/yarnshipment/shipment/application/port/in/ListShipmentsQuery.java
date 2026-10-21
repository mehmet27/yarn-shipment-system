package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.Objects;
import java.util.Set;

/**
 * Sevkiyat listesi isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param statuses yalnızca bu durumlar; boşsa hepsi
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 */
public record ListShipmentsQuery(Actor actor, Set<ShipmentStatus> statuses, int page, int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListShipmentsQuery {
        Objects.requireNonNull(actor, "actor");
        statuses = Set.copyOf(statuses);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }
}
