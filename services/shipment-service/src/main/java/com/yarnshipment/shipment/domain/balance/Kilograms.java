package com.yarnshipment.shipment.domain.balance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Kilolar 3 ondalıklı ondalık sayıdır, kayan nokta kullanılmaz (KR-04); yuvarlama yalnız sonuçta, HALF_UP (8.3). */
public final class Kilograms {

    public static final int SCALE = 3;
    private static final int MAX_INTEGER_DIGITS = 9;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private Kilograms() {}

    /** Değeri 3 ondalığa yuvarlar. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Metni sıfırdan büyük kiloya çevirir; en çok 3 ondalık kabul edilir, aksi halde alan adıyla istisna. */
    public static BigDecimal parsePositive(String text, String field) {
        try {
            BigDecimal value = new BigDecimal(text.strip());
            if (value.signum() <= 0
                    || value.scale() > SCALE
                    || value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
                throw new InvalidLedgerInputException(field);
            }
            return round(value);
        } catch (NumberFormatException e) {
            throw new InvalidLedgerInputException(field, e);
        }
    }
}
