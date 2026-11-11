package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fire dosyası listesi isteği.
 *
 * @param actor isteyen
 * @param status durum filtresi
 * @param subcontractorId fasoncu filtresi
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 */
public record ListFireSettlementsQuery(
        Actor actor,
        @Nullable FireSettlementStatus status,
        @Nullable UUID subcontractorId,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListFireSettlementsQuery {
        Objects.requireNonNull(actor, "actor");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }
}
