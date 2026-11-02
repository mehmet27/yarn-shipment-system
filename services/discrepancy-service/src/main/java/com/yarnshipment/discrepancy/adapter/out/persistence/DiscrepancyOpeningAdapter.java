package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.DiscrepancyOpeningPort;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import org.springframework.stereotype.Component;

/** {@link DiscrepancyOpeningPort}'un JDBC uygulaması; yazım {@link DiscrepancyWriter} üzerinden (EVT-03). */
@Component
final class DiscrepancyOpeningAdapter implements DiscrepancyOpeningPort {

    private final DiscrepancyWriter writer;

    DiscrepancyOpeningAdapter(DiscrepancyWriter writer) {
        this.writer = writer;
    }

    @Override
    public void openDiscrepancy(OpenedDiscrepancy discrepancy) {
        WatchedShipment shipment = discrepancy.shipment();
        writer.setActor(discrepancy.actorId());
        writer.open(
                new DiscrepancyWriter.Target(
                        shipment.siteId(), shipment.shipmentId(), shipment.shipmentNo(), shipment.subcontractorId()),
                discrepancy.finding(),
                discrepancy.dueAt(),
                new DiscrepancyWriter.Stamp(discrepancy.actorId(), discrepancy.openedAt()));
    }
}
