package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Her listeden bir kayıt döner ve hangi listelerin okunduğunu not eder (kör giriş testi için). */
final class StubShipmentBagsPort implements ShipmentBagsPort {

    private static final Instant AT = Instant.parse("2026-10-21T09:00:00Z");

    private final List<String> reads = new ArrayList<>();

    List<String> reads() {
        return List.copyOf(reads);
    }

    @Override
    public List<DispatchBag> findDispatchBags(UUID shipmentId) {
        reads.add("dispatch");
        return List.of(new DispatchBag(UUID.randomUUID(), "K-1", "LOT-1", "A", ScanMethod.SCAN, AT));
    }

    @Override
    public List<DeliveryScan> findDeliveryScans(UUID shipmentId) {
        reads.add("delivery");
        return List.of(new DeliveryScan("K-1", true, ScanMethod.SCAN, AT));
    }
}
