package com.yarnshipment.shipment.application.port.in.stock;

import java.math.BigDecimal;

/**
 * İpliğin toplam dağılımı: gelen iplik merkezde mi, fasoncularda mı, merkez üretimine mi gitti.
 *
 * @param receivedKg merkeze girmiş ve kapıdan aktarılmış toplam iplik (kg)
 * @param directTransferKg bunun merkez depoya girmeden kapıdan fasoncuya aktarılan kısmı (kg)
 * @param atSubcontractorsKg fasoncuların elindeki iplik, yani bakiyelerin toplamı (kg)
 * @param usedInCentralProductionKg merkez fabrikada kumaş üretimine verilen iplik (kg)
 */
public record YarnFlow(
        BigDecimal receivedKg,
        BigDecimal directTransferKg,
        BigDecimal atSubcontractorsKg,
        BigDecimal usedInCentralProductionKg) {}
