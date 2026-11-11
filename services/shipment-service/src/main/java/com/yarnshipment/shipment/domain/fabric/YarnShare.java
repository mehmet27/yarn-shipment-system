package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;

/**
 * Kumaşın tükettiği bir iplik cinsi.
 *
 * @param yarnType iplik cinsi
 * @param netKg tüketilen net kg, sıfırdan büyük
 */
public record YarnShare(String yarnType, BigDecimal netKg) {

    public YarnShare {
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarns.yarnType");
        }
        netKg = Kilograms.round(netKg);
        if (netKg.signum() <= 0) {
            throw new InvalidLedgerInputException("yarns.netKg");
        }
    }
}
