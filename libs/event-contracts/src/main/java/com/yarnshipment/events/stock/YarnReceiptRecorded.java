package com.yarnshipment.events.stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * MerkezStokGirisiYapildi: merkez iplik stokuna giriş yazıldı. Serbest metin ve kişisel veri taşımaz (EVT-08).
 *
 * @param movementId stok hareketi
 * @param yarnType iplik cinsi
 * @param netKg giriş net kg
 * @param receivedAt giriş zamanı
 */
public record YarnReceiptRecorded(UUID movementId, String yarnType, BigDecimal netKg, Instant receivedAt) {

    public static final String TYPE = "com.yarnshipment.stock.yarn-receipt-recorded.v1";
}
