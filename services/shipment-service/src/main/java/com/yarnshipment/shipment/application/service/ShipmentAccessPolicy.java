package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentDays;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Sevkiyata özgü erişim kuralları (yetkilendirme analizi, "Veri seviyesi kurallar"): isteyenin hangi görünümü
 * alacağı (kör giriş) ve kaydın durumuna göre hangi işlemleri yapabileceği. Kapsam kontrolü {@code ScopeGuard}'dadır.
 */
@Component
public final class ShipmentAccessPolicy {

    private final Clock clock;

    public ShipmentAccessPolicy(Clock clock) {
        this.clock = clock;
    }

    public ShipmentAudience audienceOf(Actor actor) {
        boolean internal = actor.hasAnyRole(Role.QUALITY, Role.MANAGER, Role.GENERAL_MANAGER, Role.SYSTEM);
        return internal ? ShipmentAudience.INTERNAL : ShipmentAudience.WAREHOUSE;
    }

    /** Durum, izin tablosu ve sevkiyat gününe göre izin verilen işlemler. */
    public Set<ShipmentAction> allowedActions(Shipment shipment, Actor actor) {
        boolean due = !ShipmentDays.today(clock).isBefore(shipment.shipmentDate());
        return Arrays.stream(ShipmentAction.values())
                .filter(action -> action.isAllowedIn(shipment.status()))
                .filter(action -> due || !action.requiresShipmentDay())
                .filter(action -> actor.hasPermission(action.requiredPermission()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ShipmentAction.class)));
    }
}
