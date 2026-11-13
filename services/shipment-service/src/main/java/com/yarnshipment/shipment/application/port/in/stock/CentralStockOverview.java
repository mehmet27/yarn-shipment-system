package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.List;

/**
 * Merkez iplik stoku özeti.
 *
 * @param rows iplik cinsi başına eldeki kilo
 * @param totalKg eldeki toplam kilo
 * @param producedFabricKg merkez fabrikada bugüne kadar üretilen toplam kumaş (kg)
 * @param flow ipliğin merkez / fasoncu / üretim dağılımı
 * @param recent son hareketler, yeniden eskiye
 */
public record CentralStockOverview(
        List<CentralStockRow> rows,
        BigDecimal totalKg,
        BigDecimal producedFabricKg,
        YarnFlow flow,
        List<CentralStockMovement> recent) {}
