package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import org.jspecify.annotations.Nullable;

/** İsteğe bağlı kısa metin: kenar boşlukları atılır, boşsa yok sayılır, en çok 50 karakter. */
public final class OptionalText {

    public static final int MAX_LENGTH = 50;

    private OptionalText() {}

    public static @Nullable String clean(String field, @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > MAX_LENGTH) {
            throw new InvalidMasterDataException(field, "too long");
        }
        return text;
    }
}
