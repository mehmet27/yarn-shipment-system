package com.yarnshipment.shipment.domain.stock;

/** Merkez stok hareketinin türü; işaret türden gelir. */
public enum CentralStockKind {
    /** Merkeze iplik girdi (+). */
    YARN_RECEIPT,
    /** Merkezden fasoncuya sevk edildi (-). */
    TO_SUBCONTRACTOR,
    /** Merkez fabrikada kumaşa dönüştü, fire dahil (-). */
    PRODUCTION
}
