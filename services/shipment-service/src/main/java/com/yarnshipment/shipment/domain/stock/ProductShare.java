package com.yarnshipment.shipment.domain.stock;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Merkez kumaşın tükettiği bir iplik tanımı.
 *
 * @param productId iplik tanımı
 * @param yarnType iplik tanımının adı
 * @param netKg tüketilen net kg, sıfırdan büyük
 */
public record ProductShare(UUID productId, String yarnType, BigDecimal netKg) {

    public ProductShare {
        Objects.requireNonNull(productId, "productId");
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarns.productId");
        }
        netKg = Kilograms.round(netKg);
        if (netKg.signum() <= 0) {
            throw new InvalidLedgerInputException("yarns.netKg");
        }
    }
}
