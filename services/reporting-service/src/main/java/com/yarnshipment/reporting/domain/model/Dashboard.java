package com.yarnshipment.reporting.domain.model;

import java.util.List;

/**
 * Panel ana sayfa özeti.
 *
 * @param period dönem
 * @param shipments sevkiyat durum dağılımı
 * @param quantity kilo toplamları
 * @param bands bant dağılımı
 * @param discrepancies açık uyuşmazlıklar
 * @param overdueDiscrepancies vadesi en çok geçenler
 * @param largestDifferences mutlak farkı en büyük sevkiyatlar
 */
public record Dashboard(
        ReportPeriod period,
        ShipmentTotals shipments,
        QuantitySummary quantity,
        BandCounts bands,
        OpenDiscrepancies discrepancies,
        List<OverdueDiscrepancy> overdueDiscrepancies,
        List<LargestDifference> largestDifferences) {

    public Dashboard {
        overdueDiscrepancies = List.copyOf(overdueDiscrepancies);
        largestDifferences = List.copyOf(largestDifferences);
    }
}
