package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.model.BagView;
import com.yarnshipment.bag.domain.model.Bag;

/** Çuvalın web modeli. */
final class BagViews {

    private BagViews() {}

    static BagView toView(Bag bag) {
        return new BagView(
                        bag.id(),
                        bag.uniqueKey(),
                        com.yarnshipment.bag.adapter.in.web.api.model.PackageType.valueOf(
                                bag.packageType().name()),
                        bag.lotNo(),
                        bag.qualityGradeCode(),
                        com.yarnshipment.bag.adapter.in.web.api.model.BagStatus.valueOf(
                                bag.status().name()))
                .reservedShipmentId(bag.reservedShipmentId());
    }
}
