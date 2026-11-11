package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bir fasoncunun sevkiyat göstergeleri.
 *
 * @param id fasoncu
 * @param shipmentCount çıkış tartısı yapılmış sevkiyat
 * @param discrepantShipmentCount uyuşmazlık çıkan sevkiyat
 */
public record PartyStats(UUID id, int shipmentCount, int discrepantShipmentCount) {

    /** Uyuşmazlıklı sevkiyat yüzdesi, iki ondalık. */
    public BigDecimal discrepancyRate() {
        return Rates.percent(discrepantShipmentCount, shipmentCount);
    }
}
