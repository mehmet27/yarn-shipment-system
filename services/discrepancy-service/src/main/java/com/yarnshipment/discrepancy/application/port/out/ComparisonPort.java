package com.yarnshipment.discrepancy.application.port.out;

import java.util.UUID;

/**
 * Karşılaştırma girdisini, açılan uyuşmazlıkları ve sonuç olayını aynı transaction'da yazar (EVT-03). Teslimden önce
 * açılmış ve hâlâ çözülmemiş uyuşmazlık (fotoğraf, zaman) varsa sonuç bulgusuz da olsa {@code DISCREPANCY} olur:
 * sevkiyat incelemeye alınır ve son uyuşmazlık çözülünce tamamlanır.
 */
public interface ComparisonPort {

    /** Sevkiyat daha önce karşılaştırıldı mı; aynı sevkiyat ikinci kez karşılaştırılmaz. */
    boolean hasComparison(UUID shipmentId);

    void saveComparison(CompletedComparison comparison);
}
