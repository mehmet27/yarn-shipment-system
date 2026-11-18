package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.shipment.domain.stock.CentralStockEntry;
import com.yarnshipment.shipment.domain.stock.SupplierReceipt;
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
 * @param suppliers tedarikçi ve iplik cinsi başına bugüne kadar gelen iplik
 */
public record CentralStockOverview(
        List<CentralStockRow> rows,
        BigDecimal totalKg,
        BigDecimal producedFabricKg,
        YarnFlow flow,
        List<CentralStockEntry> recent,
        List<SupplierReceipt> suppliers) {}
