package com.yarnshipment.shipment.domain.check;

/** discrepancy-service karşılaştırmasının sonucu (olay kataloğu: KarsilastirmaTamamlandi). */
public enum ComparisonOutcome {
    /** Uyuşmazlık yok; sevkiyat tamamlanır. */
    CLEAN,
    /** En az bir uyuşmazlık açıldı; sevkiyat incelemeye düşer. */
    DISCREPANCY
}
