package com.yarnshipment.reporting.domain.model;

import java.util.List;

/**
 * Panel ana sayfa özeti.
 *
 * @param period dönem
 * @param shipments sevkiyat durum dağılımı
 * @param quantity kilo toplamları
 * @param discrepancies açık uyuşmazlıklar
 * @param overdueDiscrepancies vadesi en çok geçenler
 */
public record Dashboard(
        ReportPeriod period,
        ShipmentTotals shipments,
        QuantitySummary quantity,
        OpenDiscrepancies discrepancies,
        List<OverdueDiscrepancy> overdueDiscrepancies) {

    public Dashboard {
        overdueDiscrepancies = List.copyOf(overdueDiscrepancies);
    }
}
