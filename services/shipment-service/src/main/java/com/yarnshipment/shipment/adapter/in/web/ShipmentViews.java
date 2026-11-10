package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.Audience;
import com.yarnshipment.shipment.adapter.in.web.api.model.InternalShipmentView;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentDetailResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorShipmentView;
import com.yarnshipment.shipment.adapter.in.web.api.model.WarehouseShipmentView;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.domain.model.Shipment;

/** Sevkiyatı isteyenin görünümüne çevirir (API-09, API-11). Her görünüm kendi alanlarını açıkça doldurur. */
final class ShipmentViews {

    private ShipmentViews() {}

    static ShipmentDetailResponse toDetail(ShipmentDetails details) {
        return switch (details.audience()) {
            case INTERNAL -> toInternal(details);
            case WAREHOUSE -> toWarehouse(details);
            case SUBCONTRACTOR -> toSubcontractor(details);
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
                .dispatchWeighing(WebMappings.weighing(shipment.dispatchWeighing()))
                .waybillNo(shipment.waybillNo())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .fireRate(WebMappings.fireRate(shipment.fireRate()))
                .lines(WebMappings.lines(shipment.lines()))
                .weightCheck(WebMappings.weightCheck(shipment.weightCheck()))
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
                .dispatchWeighing(WebMappings.weighing(shipment.dispatchWeighing()))
                .waybillNo(shipment.waybillNo())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .fireRate(WebMappings.fireRate(shipment.fireRate()))
                .lines(WebMappings.lines(shipment.lines()))
                .weightCheck(WebMappings.weightCheck(shipment.weightCheck()))
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
                .dispatchWeighing(WebMappings.weighing(shipment.dispatchWeighing()))
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(details.allowedActions()));
    }
}
