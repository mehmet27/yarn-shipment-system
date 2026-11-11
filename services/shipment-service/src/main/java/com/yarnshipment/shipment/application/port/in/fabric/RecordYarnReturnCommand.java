package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.ReturnDirection;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İplik dönüşü ya da fasoncudan gelen iplik isteği.
 *
 * @param actor kaydeden (SEC-04)
 * @param shipmentId ilgili sevkiyat
 * @param direction yön
 * @param yarnType iplik cinsi
 * @param lotId lot, isteğe bağlı
 * @param netKg net kg
 * @param waybillNo irsaliye numarası, isteğe bağlı
 */
public record RecordYarnReturnCommand(
        Actor actor,
        UUID shipmentId,
        ReturnDirection direction,
        String yarnType,
        @Nullable UUID lotId,
        BigDecimal netKg,
        @Nullable String waybillNo) {

    public RecordYarnReturnCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(yarnType, "yarnType");
        Objects.requireNonNull(netKg, "netKg");
    }
}
