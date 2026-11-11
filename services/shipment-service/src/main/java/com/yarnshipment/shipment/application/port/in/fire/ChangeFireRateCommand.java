package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Fire oranı değişikliği.
 *
 * @param actor değiştiren (SEC-04)
 * @param shipmentId sevkiyat
 * @param fireRate yeni oran, 0 ile 0,20 arası
 * @param reason gerekçe; denetim kaydına yazılır (KR-02)
 */
public record ChangeFireRateCommand(Actor actor, UUID shipmentId, BigDecimal fireRate, String reason) {

    public ChangeFireRateCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(fireRate, "fireRate");
        Objects.requireNonNull(reason, "reason");
    }
}
