package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Kurulum ayarları: yeni sevkiyatın varsayılan fire oranı ve kantar toleransı.
 *
 * @param defaultFireRate varsayılan fire oranı, 0 ile 0.20 arası (0.04 = %4), en çok 4 ondalık
 * @param weightToleranceKg kantar toleransı kg, sıfır ya da büyük, en çok 3 ondalık
 */
public record ShipmentSettings(BigDecimal defaultFireRate, BigDecimal weightToleranceKg) {

    public static final BigDecimal MAX_FIRE_RATE = new BigDecimal("0.20");
    public static final ShipmentSettings DEFAULTS =
            new ShipmentSettings(new BigDecimal("0.0400"), new BigDecimal("0.000"));

    public ShipmentSettings {
        Objects.requireNonNull(defaultFireRate, "defaultFireRate");
        Objects.requireNonNull(weightToleranceKg, "weightToleranceKg");
        requireFireRate(defaultFireRate, "defaultFireRate");
        if (weightToleranceKg.signum() < 0
                || weightToleranceKg.stripTrailingZeros().scale() > 3) {
            throw new InvalidNewShipmentException("weightToleranceKg", "must be >= 0 with at most 3 decimals");
        }
        defaultFireRate = defaultFireRate.setScale(4);
        weightToleranceKg = weightToleranceKg.setScale(3);
    }

    /** Fire oranı 0..%20 ve en çok 4 ondalık olmalıdır. */
    public static BigDecimal requireFireRate(BigDecimal rate, String field) {
        if (rate.signum() < 0
                || rate.compareTo(MAX_FIRE_RATE) > 0
                || rate.stripTrailingZeros().scale() > 4) {
            throw new InvalidNewShipmentException(field, "fire rate must be between 0 and 0.20");
        }
        return rate.setScale(4);
    }
}
