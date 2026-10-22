package com.yarnshipment.shipment.masterdata.domain.model;

import java.util.Locale;

/** Plaka kuralı: boşluklar atılır, büyük harfe çevrilir, boş olamaz, en çok 20 karakter. */
public final class PlateNumber {

    private static final int MAX_LENGTH = 20;

    private PlateNumber() {}

    public static String normalize(String value) {
        String plate = value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (plate.isEmpty()) {
            throw new InvalidMasterDataException("plateNo", "blank");
        }
        if (plate.length() > MAX_LENGTH) {
            throw new InvalidMasterDataException("plateNo", "too long");
        }
        return plate;
    }
}
