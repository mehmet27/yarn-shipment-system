package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.domain.model.Shipment;
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

    /** Dış roller önce: dış rol iç rolle aynı token'da bulunamaz (common-security). */
    public ShipmentAudience audienceOf(Actor actor) {
        if (actor.hasRole(Role.SUBCONTRACTOR)) {
            return ShipmentAudience.SUBCONTRACTOR;
        }
        if (actor.hasRole(Role.DRIVER)) {
            return ShipmentAudience.DRIVER;
        }
        boolean seesBothSides = actor.hasAnyRole(Role.ADMIN, Role.QUALITY, Role.MANAGER, Role.SYSTEM);
        return seesBothSides ? ShipmentAudience.INTERNAL : ShipmentAudience.WAREHOUSE;
    }

    /** Durum ve izin tablosuna göre izin verilen işlemler. */
    public Set<ShipmentAction> allowedActions(Shipment shipment, Actor actor) {
        return Arrays.stream(ShipmentAction.values())
                .filter(action -> action.isAllowedIn(shipment.status()))
                .filter(action -> actor.hasPermission(action.requiredPermission()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ShipmentAction.class)));
    }
}
