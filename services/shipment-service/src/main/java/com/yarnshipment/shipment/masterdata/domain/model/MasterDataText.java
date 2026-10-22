package com.yarnshipment.shipment.masterdata.domain.model;

/** Ad ve unvan alanlarının ortak kuralı: kenar boşlukları atılır, boş olamaz, en çok 200 karakter. */
public final class MasterDataText {

    public static final int MAX_LENGTH = 200;

    private MasterDataText() {}

    public static String clean(String field, String value) {
        String text = value.strip();
        if (text.isEmpty()) {
            throw new InvalidMasterDataException(field, "blank");
        }
        if (text.length() > MAX_LENGTH) {
            throw new InvalidMasterDataException(field, "too long");
        }
        return text;
    }
}
