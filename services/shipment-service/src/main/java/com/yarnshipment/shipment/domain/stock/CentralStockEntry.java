package com.yarnshipment.shipment.domain.stock;

import org.jspecify.annotations.Nullable;

/**
 * Defterden okunan hareket ve iplik girişiyse tedarikçi bilgisi.
 *
 * @param movement hareket
 * @param receipt iplik girişinin tedarikçi bilgisi; çıkışlarda, sistem girişlerinde ve eski satırlarda boş
 */
public record CentralStockEntry(
        CentralStockMovement movement, @Nullable ReceiptDetails receipt) {}
