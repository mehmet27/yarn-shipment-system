package com.yarnshipment.shipment.domain.stock;

import java.io.Serial;
import java.math.BigDecimal;

/** Merkez fabrika üretimi için stokta yeterli iplik yok (409, {@code INSUFFICIENT_CENTRAL_STOCK}). */
public final class InsufficientCentralStockException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String yarnType;

    public InsufficientCentralStockException(String yarnType, BigDecimal availableKg) {
        super("Insufficient central stock yarnType=" + yarnType + " availableKg=" + availableKg.toPlainString());
        this.yarnType = yarnType;
    }

    public String yarnType() {
        return yarnType;
    }
}
