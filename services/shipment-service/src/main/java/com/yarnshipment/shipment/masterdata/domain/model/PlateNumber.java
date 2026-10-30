package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Plaka kuralı: boşluklar atılır, büyük harfe çevrilir, boş olamaz. Sonuç Türkiye plaka biçiminde olmalıdır: 01-81
 * il kodu, 1-3 harf, 2-4 rakam (ör. {@code 34ABC123}). Ön yüz aynı deseni kullanır.
 */
public final class PlateNumber {

    private static final Pattern TURKISH_PLATE = Pattern.compile("(0[1-9]|[1-7][0-9]|8[01])[A-Z]{1,3}[0-9]{2,4}");

    private PlateNumber() {}

    public static String normalize(String value) {
        String plate = value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (plate.isEmpty()) {
            throw new InvalidMasterDataException("plateNo", "blank");
        }
        if (!TURKISH_PLATE.matcher(plate).matches()) {
            throw new InvalidPlateException(plate);
        }
        return plate;
    }
}
