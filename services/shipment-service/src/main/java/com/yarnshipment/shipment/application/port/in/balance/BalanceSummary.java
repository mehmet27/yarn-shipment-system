package com.yarnshipment.shipment.application.port.in.balance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bakiye ekranı.
 *
 * @param items fasoncu ve iplik cinsi başına satırlar
 * @param totalKg satırların bakiye toplamı
 */
public record BalanceSummary(List<BalanceRow> items, BigDecimal totalKg) {

    public BalanceSummary {
        items = List.copyOf(items);
    }
}
