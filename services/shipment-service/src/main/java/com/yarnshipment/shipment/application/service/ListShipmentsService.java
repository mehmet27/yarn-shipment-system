package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentListItem;
import com.yarnshipment.shipment.application.port.in.ShipmentPage;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.PageRequest;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.ShipmentSlice;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aktörün kapsamını sorguya ekleyerek sevkiyatları listeler (SEC-03). */
@Service
public class ListShipmentsService implements ListShipmentsUseCase {

    private final ShipmentQueryPort shipmentQueryPort;
    private final ScopeGuard scopeGuard;
    private final ShipmentAccessPolicy accessPolicy;

    public ListShipmentsService(
            ShipmentQueryPort shipmentQueryPort, ScopeGuard scopeGuard, ShipmentAccessPolicy accessPolicy) {
        this.shipmentQueryPort = shipmentQueryPort;
        this.scopeGuard = scopeGuard;
        this.accessPolicy = accessPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentPage listShipments(ListShipmentsQuery query) {
        Actor actor = query.actor();
        ShipmentSlice slice = shipmentQueryPort.findShipments(
                scopeGuard.scopeOf(actor), query.statuses(), new PageRequest(query.page(), query.size()));
        List<ShipmentListItem> items = slice.shipments().stream()
                .map(shipment -> new ShipmentListItem(shipment, accessPolicy.allowedActions(shipment, actor)))
                .toList();
        return new ShipmentPage(items, query.page(), query.size(), slice.totalElements());
    }
}
