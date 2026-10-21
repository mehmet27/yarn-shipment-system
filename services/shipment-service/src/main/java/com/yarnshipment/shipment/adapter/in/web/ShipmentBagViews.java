package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.Audience;
import com.yarnshipment.shipment.adapter.in.web.api.model.DeliveryScanView;
import com.yarnshipment.shipment.adapter.in.web.api.model.DispatchBagView;
import com.yarnshipment.shipment.adapter.in.web.api.model.ScanMethod;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentBagsResponse;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;

/**
 * Çuval görünümü (SEC-05): görünüme ait olmayan liste ve {@code matched} alanı JSON'da hiç yer almaz. {@code matched}
 * çıkış listesine göre sonuç verdiği için fasoncuya gösterilmez.
 */
final class ShipmentBagViews {

    private ShipmentBagViews() {}

    static ShipmentBagsResponse toResponse(ShipmentBags bags) {
        ShipmentBagsResponse response =
                new ShipmentBagsResponse(Audience.valueOf(bags.audience().name()));
        // Üretilen model listeleri boş başlatır; görünüme ait olmayan liste boş değil hiç olmamalı.
        response.setDispatchBags(null);
        response.setDeliveryScans(null);
        if (bags.dispatchBags() != null) {
            response.dispatchBags(
                    bags.dispatchBags().stream().map(ShipmentBagViews::toView).toList());
        }
        if (bags.deliveryScans() != null) {
            boolean showMatch = bags.audience() == ShipmentAudience.INTERNAL;
            response.deliveryScans(bags.deliveryScans().stream()
                    .map(scan -> toView(scan, showMatch))
                    .toList());
        }
        return response;
    }

    private static DispatchBagView toView(DispatchBag bag) {
        return new DispatchBagView(
                bag.bagId(),
                bag.uniqueKey(),
                bag.lotNo(),
                bag.qualityGradeCode(),
                ScanMethod.valueOf(bag.scanMethod().name()),
                WebMappings.utc(bag.assignedAt()));
    }

    private static DeliveryScanView toView(DeliveryScan scan, boolean showMatch) {
        DeliveryScanView view = new DeliveryScanView(
                scan.scannedKey(), ScanMethod.valueOf(scan.scanMethod().name()), WebMappings.utc(scan.scannedAt()));
        return showMatch ? view.matched(scan.matched()) : view;
    }
}
