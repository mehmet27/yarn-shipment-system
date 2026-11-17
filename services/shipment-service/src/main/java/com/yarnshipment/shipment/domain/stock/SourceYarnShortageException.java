package com.yarnshipment.shipment.domain.stock;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * Fasoncudan fasoncuya sevkiyatta gönderen fasoncunun elinde yeterli iplik yok (409, {@code SOURCE_YARN_SHORTAGE});
 * sevkiyat taslağı bile açılamaz.
 */
public final class SourceYarnShortageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<Shortage> shortages;

    public SourceYarnShortageException(List<Shortage> shortages) {
        super("Source subcontractor lacks yarn for " + shortages.size() + " yarn(s)");
        this.shortages = List.copyOf(shortages);
    }

    public List<Shortage> shortages() {
        return shortages;
    }

    /** Yetmeyen iplik; eksik kilo istenen ile eldeki kilo arasındaki farktır. */
    public record Shortage(String yarnType, BigDecimal missingKg) {}
}
