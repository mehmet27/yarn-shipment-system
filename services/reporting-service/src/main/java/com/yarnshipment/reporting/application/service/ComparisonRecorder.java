package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.application.port.out.ReadModelPort.PairComparison;
import com.yarnshipment.reporting.domain.model.ReadModelChange.ComparisonCompleted;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentNotInReadModelException;
import com.yarnshipment.reporting.domain.model.ShipmentParties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Karşılaştırma sonucunu sevkiyata ve şoför–fasoncu günlük sayaçlarına yazar. Çağıranın transaction'ında çalışır. */
@Component
final class ComparisonRecorder {

    private final ReadModelPort readModel;

    ComparisonRecorder(ReadModelPort readModel) {
        this.readModel = readModel;
    }

    void record(ComparisonCompleted comparison) {
        ShipmentParties parties = readModel
                .findParties(comparison.shipmentId())
                .orElseThrow(() -> new ShipmentNotInReadModelException(comparison.shipmentId()));
        readModel.saveComparison(comparison);
        UUID driverId = parties.driverId();
        if (driverId == null) {
            // Şoför bilinmiyorsa eşleşme kurulamaz; sevkiyat yine de şoför dışı raporlarda sayılır.
            return;
        }
        LocalDate day = LocalDate.ofInstant(comparison.comparedAt(), ReportPeriod.ZONE);
        BigDecimal absoluteDiff = comparison.diffKg() == null
                ? BigDecimal.ZERO
                : comparison.diffKg().abs();
        readModel.addPairComparison(new PairComparison(
                comparison.comparedAt(),
                day,
                parties.siteId(),
                driverId,
                parties.subcontractorId(),
                comparison.discrepancyCount(),
                absoluteDiff));
    }
}
