package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.Audience;
import com.yarnshipment.shipment.adapter.in.web.api.model.DriverShipmentView;
import com.yarnshipment.shipment.adapter.in.web.api.model.InternalShipmentView;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentDetailResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorShipmentView;
import com.yarnshipment.shipment.adapter.in.web.api.model.WarehouseShipmentView;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentSide;

/**
 * Sevkiyatı isteyenin görünümüne çevirir (API-09, API-11). Her görünüm kendi alanlarını açıkça doldurur; kör girişe
 * tabi alanlar fasoncu, şoför ve depo görünümlerinde tipte hiç yoktur (SEC-05).
 */
final class ShipmentViews {

    private ShipmentViews() {}

    static ShipmentDetailResponse toDetail(ShipmentDetails details) {
        return switch (details.audience()) {
            case INTERNAL -> toInternal(details);
            case WAREHOUSE -> toWarehouse(details);
            case SUBCONTRACTOR -> toSubcontractor(details);
            case DRIVER -> toDriver(details);
        };
    }

    private static InternalShipmentView toInternal(ShipmentDetails details) {
        Shipment shipment = details.shipment();
        return new InternalShipmentView()
                .audience(Audience.INTERNAL)
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorId(shipment.subcontractorId())
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .driverName(shipment.parties().driverName())
                .plateNo(shipment.parties().plateNo())
                .plannedBagCount(shipment.planned().bagCount())
                .plannedNetKg(WebMappings.kgOrNull(shipment.planned().netKg()))
                .dispatchWeighing(WebMappings.weighing(shipment.dispatch().weighing()))
                .dispatchSealNo(shipment.dispatch().sealNo())
                .deliveryWeighing(WebMappings.weighing(shipment.delivery().weighing()))
                .deliverySealCondition(
                        WebMappings.sealCondition(shipment.delivery().sealCondition()))
                .randomCheckSelected(shipment.randomCheckSelected())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(details.allowedActions()));
    }

    private static WarehouseShipmentView toWarehouse(ShipmentDetails details) {
        Shipment shipment = details.shipment();
        return new WarehouseShipmentView()
                .audience(Audience.WAREHOUSE)
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorId(shipment.subcontractorId())
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .driverName(shipment.parties().driverName())
                .plateNo(shipment.parties().plateNo())
                .plannedBagCount(shipment.planned().bagCount())
                .plannedNetKg(WebMappings.kgOrNull(shipment.planned().netKg()))
                .dispatchWeighing(WebMappings.weighing(shipment.dispatch().weighing()))
                .dispatchSealNo(shipment.dispatch().sealNo())
                .randomCheckSelected(shipment.randomCheckSelected())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(details.allowedActions()));
    }

    private static SubcontractorShipmentView toSubcontractor(ShipmentDetails details) {
        Shipment shipment = details.shipment();
        return new SubcontractorShipmentView()
                .audience(Audience.SUBCONTRACTOR)
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .driverName(shipment.parties().driverName())
                .plateNo(shipment.parties().plateNo())
                .deliveryWeighing(WebMappings.weighing(shipment.delivery().weighing()))
                .deliverySealCondition(
                        WebMappings.sealCondition(shipment.delivery().sealCondition()))
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(details.allowedActions()));
    }

    private static DriverShipmentView toDriver(ShipmentDetails details) {
        Shipment shipment = details.shipment();
        ShipmentSide dispatch = shipment.dispatch();
        return new DriverShipmentView()
                .audience(Audience.DRIVER)
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .plateNo(shipment.parties().plateNo())
                .dispatchBagCount(
                        dispatch.weighing() == null ? null : dispatch.weighing().bagCount())
                .dispatchSealNo(dispatch.sealNo())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(details.allowedActions()));
    }
}
