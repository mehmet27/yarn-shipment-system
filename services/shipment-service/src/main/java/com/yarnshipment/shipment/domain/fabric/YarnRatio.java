package com.yarnshipment.shipment.domain.fabric;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kaliteci kumaş girerken bir ipliğe yazdığı oran.
 *
 * @param yarnType iplik cinsinin adı
 * @param lotId iplik partisi; partisiz iplikte boş
 * @param ratioPct kumaşın bu iplikten yapılan yüzdesi, en çok iki ondalık, 0'dan büyük
 */
public record YarnRatio(String yarnType, @Nullable UUID lotId, BigDecimal ratioPct) {

    public static final int SCALE = 2;
    public static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public YarnRatio {
        yarnType = yarnType.strip();
        if (yarnType.isEmpty()) {
            throw new InvalidLedgerInputException("yarns.yarnType");
        }
        boolean outOfRange = ratioPct.signum() <= 0 || ratioPct.compareTo(HUNDRED) > 0;
        if (outOfRange || ratioPct.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidLedgerInputException("yarns.ratioPct");
        }
        ratioPct = ratioPct.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
