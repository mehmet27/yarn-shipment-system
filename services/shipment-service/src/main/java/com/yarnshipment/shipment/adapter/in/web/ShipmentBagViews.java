package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.Audience;
import com.yarnshipment.shipment.adapter.in.web.api.model.DispatchBagView;
import com.yarnshipment.shipment.adapter.in.web.api.model.PackagingType;
import com.yarnshipment.shipment.adapter.in.web.api.model.ScanMethod;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentBagsResponse;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.domain.model.DispatchBag;

/** Sevkiyattaki çuvalların görünümü. */
final class ShipmentBagViews {

    private ShipmentBagViews() {}

    static ShipmentBagsResponse toResponse(ShipmentBags bags) {
        return new ShipmentBagsResponse(
                Audience.valueOf(bags.audience().name()),
                bags.dispatchBags().stream().map(ShipmentBagViews::toView).toList());
    }

    private static DispatchBagView toView(DispatchBag bag) {
        return new DispatchBagView(
                bag.bagId(),
                bag.uniqueKey(),
                PackagingType.valueOf(bag.packageType().name()),
                bag.lotNo(),
                bag.qualityGradeCode(),
                ScanMethod.valueOf(bag.scanMethod().name()),
                WebMappings.utc(bag.assignedAt()));
    }
}
