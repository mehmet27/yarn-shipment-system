package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Faturalananlar listesi isteği: fatura tarihine ve fasoncuya göre süzülür.
 *
 * @param actor isteyen
 * @param from fatura tarihi başlangıcı (dahil)
 * @param to fatura tarihi bitişi (dahil)
 * @param subcontractorId fasoncu filtresi
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 */
public record ListFireInvoicesQuery(
        Actor actor,
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable UUID subcontractorId,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListFireInvoicesQuery {
        Objects.requireNonNull(actor, "actor");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }
}
