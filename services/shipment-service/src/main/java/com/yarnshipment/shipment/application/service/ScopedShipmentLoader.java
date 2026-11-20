package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Yazma işlemlerinden önce sevkiyatı yükler ve isteyenin kapsamında olduğunu doğrular (SEC-03). */
@Component
public final class ScopedShipmentLoader {

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentQueryPort shipmentQueryPort;
    private final ScopeGuard scopeGuard;

    public ScopedShipmentLoader(ShipmentQueryPort shipmentQueryPort, ScopeGuard scopeGuard) {
        this.shipmentQueryPort = shipmentQueryPort;
        this.scopeGuard = scopeGuard;
    }

    /** Kayıt yoksa 404, kapsam dışıysa audit'e yazılan 403. Okuma ve sevkiyat satırına yazmayan işlemler içindir. */
    public Shipment load(Actor actor, UUID shipmentId, Permission permission) {
        return scoped(actor, shipmentQueryPort.findShipment(shipmentId), shipmentId, permission);
    }

    /**
     * {@link #load} ile aynı denetimler; ayrıca sevkiyat satırını transaction sonuna kadar kilitler. Sevkiyatın
     * durumunu, çuvallarını ya da reçetesini değiştiren her işlem bununla yükler: çift tıklama ve aynı anda gelen iki
     * istek sıraya girer, ikincisi güncel durumu görür.
     */
    public Shipment loadForUpdate(Actor actor, UUID shipmentId, Permission permission) {
        return scoped(actor, shipmentQueryPort.findShipmentForUpdate(shipmentId), shipmentId, permission);
    }

    private Shipment scoped(Actor actor, Optional<Shipment> found, UUID shipmentId, Permission permission) {
        Shipment shipment = found.orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        ScopedResource resource = new ScopedResource(RESOURCE_TYPE, shipment.id(), shipment.siteId());
        scopeGuard.validateInScope(actor, resource, permission.name());
        return shipment;
    }
}
