package com.yarnshipment.bag.adapter.in.web;

import com.yarnshipment.bag.adapter.in.web.api.model.BagView;
import com.yarnshipment.bag.adapter.in.web.api.model.LabeledBagView;
import com.yarnshipment.bag.adapter.in.web.api.model.LockedLotView;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.LabeledBag;
import com.yarnshipment.bag.domain.model.LockedLot;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Çuval, parti ve etiket baskısının web modelleri. */
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

    static LockedLotView toView(LockedLot lot) {
        return new LockedLotView(
                lot.lotId(), lot.siteId(), lot.lotNo(), lot.qualityGradeCode(), utc(lot.lockedAt()), lot.bagCount());
    }

    static LabeledBagView toView(LabeledBag labeled) {
        Instant lastPrintedAt = labeled.lastPrintedAt();
        return new LabeledBagView(toView(labeled.bag()), labeled.printCount())
                .lastPrintedAt(lastPrintedAt == null ? null : utc(lastPrintedAt));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
