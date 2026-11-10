package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import com.yarnshipment.shipment.domain.packaging.PackageType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bir çıkış çuvalı döner ve okunduğunu not eder. */
final class StubShipmentBagsPort implements ShipmentBagsPort {

    private static final Instant AT = Instant.parse("2026-10-21T09:00:00Z");

    private final List<String> reads = new ArrayList<>();

    List<String> reads() {
        return List.copyOf(reads);
    }

    @Override
    public List<DispatchBag> findDispatchBags(UUID shipmentId) {
        reads.add("dispatch");
        return List.of(new DispatchBag(UUID.randomUUID(), "K-1", PackageType.BAG, "LOT-1", "A", ScanMethod.SCAN, AT));
    }
}
