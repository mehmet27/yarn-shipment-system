package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.PageMetadata;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentSummaryView;
import com.yarnshipment.shipment.application.port.in.ShipmentListItem;
import com.yarnshipment.shipment.application.port.in.ShipmentPage;
import com.yarnshipment.shipment.domain.model.Shipment;

/** Liste sayfasının web modeli. Liste satırı kör alan taşımaz; tüm rollerde aynıdır. */
final class ShipmentPages {

    private ShipmentPages() {}

    static ShipmentPageResponse toResponse(ShipmentPage page) {
        PageMetadata metadata = new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages());
        return new ShipmentPageResponse(
                page.items().stream().map(ShipmentPages::toSummary).toList(), metadata);
    }

    private static ShipmentSummaryView toSummary(ShipmentListItem item) {
        Shipment shipment = item.shipment();
        return new ShipmentSummaryView()
                .shipmentId(shipment.id())
                .shipmentNo(shipment.shipmentNo())
                .siteId(shipment.siteId())
                .status(WebMappings.status(shipment.status()))
                .subcontractorId(shipment.subcontractorId())
                .subcontractorTitle(shipment.parties().subcontractorTitle())
                .driverName(shipment.parties().driverName())
                .plateNo(shipment.parties().plateNo())
                .createdAt(WebMappings.utc(shipment.createdAt()))
                .allowedActions(WebMappings.actions(item.allowedActions()));
    }
}
