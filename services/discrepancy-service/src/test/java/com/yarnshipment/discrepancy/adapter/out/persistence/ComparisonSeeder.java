package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.domain.model.Finding;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Verilen bulguların uyuşmazlıklarını gerçek şemaya, test sevkiyatı için yazar. */
final class ComparisonSeeder {

    private final DiscrepancyWriter writer;

    ComparisonSeeder(DiscrepancyWriter writer) {
        this.writer = writer;
    }

    /** {@link TestComparisons#SHIPMENT} için bulgular uyuşmazlık olarak açılır. */
    void seed(List<Finding> findings, Instant at, Instant due) {
        UUID actor = SeededDiscrepancies.ACTOR;
        writer.setActor(actor);
        DiscrepancyWriter.Target target = new DiscrepancyWriter.Target(
                TestComparisons.SITE,
                TestComparisons.SHIPMENT,
                TestComparisons.SHIPMENT_NO,
                TestComparisons.SUBCONTRACTOR);
        DiscrepancyWriter.Stamp stamp = new DiscrepancyWriter.Stamp(actor, at);
        for (Finding finding : findings) {
            writer.open(target, finding, due, stamp);
        }
    }
}
