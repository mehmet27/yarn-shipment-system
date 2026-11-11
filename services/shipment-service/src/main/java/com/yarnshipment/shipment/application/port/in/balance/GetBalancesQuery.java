package com.yarnshipment.shipment.application.port.in.balance;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bakiye isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param subcontractorId yalnız bu fasoncu; boşsa hepsi
 */
public record GetBalancesQuery(Actor actor, @Nullable UUID subcontractorId) {

    public GetBalancesQuery {
        Objects.requireNonNull(actor, "actor");
    }
}
