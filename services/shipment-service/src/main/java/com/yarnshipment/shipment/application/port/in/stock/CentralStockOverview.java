package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.List;

/**
 * Merkez iplik stoku özeti.
 *
 * @param rows iplik cinsi başına eldeki kilo
 * @param totalKg eldeki toplam kilo
 * @param recent son hareketler, yeniden eskiye
 */
public record CentralStockOverview(List<CentralStockRow> rows, BigDecimal totalKg, List<CentralStockMovement> recent) {}
