package com.yarnshipment.shipment.domain.stock;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * Sevkiyat tamamlanırken merkez stok yetmiyor (409, {@code CENTRAL_STOCK_SHORTFALL}). Müdür eksik kadar sistem girişi
 * yazılmasını onaylarsa tamamlama yinelenir.
 */
public final class CentralStockShortfallException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<Shortfall> shortfalls;

    public CentralStockShortfallException(List<Shortfall> shortfalls) {
        super("Central stock shortfall for " + shortfalls.size() + " yarn(s)");
        this.shortfalls = List.copyOf(shortfalls);
    }

    public List<Shortfall> shortfalls() {
        return shortfalls;
    }

    /** Stoğu eksiye düşecek bir iplik ve eksik kilo. */
    public record Shortfall(java.util.UUID productId, String yarnType, BigDecimal missingKg) {}
}
