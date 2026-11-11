package com.yarnshipment.shipment.domain.bag;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Sevkiyata eklenecek çuvallar: her kilo bir çuvaldır ve sevkiyattaki son sıradan devam eden numarayı alır.
 *
 * @param shipmentId sevkiyat
 * @param weightsKg eklenen çuvalların kiloları, girilme sırasıyla
 */
public record BagAssignment(UUID shipmentId, List<BigDecimal> weightsKg) {

    public BagAssignment {
        weightsKg = List.copyOf(weightsKg);
    }
}
