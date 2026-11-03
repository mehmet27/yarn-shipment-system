package com.yarnshipment.discrepancy.application.port.out;

/**
 * Karşılaştırma dışında açılan uyuşmazlık (fotoğraf, zaman): satır, açılış kaydı ve {@code DiscrepancyOpened} olayı
 * aynı transaction'da yazılır (EVT-03).
 */
public interface DiscrepancyOpeningPort {

    void openDiscrepancy(OpenedDiscrepancy discrepancy);
}
