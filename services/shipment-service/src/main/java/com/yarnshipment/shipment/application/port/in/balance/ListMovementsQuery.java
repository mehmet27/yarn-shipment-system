package com.yarnshipment.shipment.application.port.in.balance;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Hareket listesi isteği.
 *
 * @param actor isteyen
 * @param subcontractorId fasoncu filtresi
 * @param yarnType iplik cinsi filtresi
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 */
public record ListMovementsQuery(
        Actor actor,
        @Nullable UUID subcontractorId,
        @Nullable String yarnType,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListMovementsQuery {
        Objects.requireNonNull(actor, "actor");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }
}
