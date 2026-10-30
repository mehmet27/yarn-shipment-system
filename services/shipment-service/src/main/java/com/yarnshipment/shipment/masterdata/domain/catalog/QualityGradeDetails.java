package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Kalite sınıfının düzenlenebilir alanları. Kod yalnızca eklerken verilir.
 *
 * @param name ad, ör. {@code Birinci kalite}
 * @param labelColor etiket rengi {@code #RRGGBB}; A/B etiketlerini renkle ayırmak için, isteğe bağlı
 */
public record QualityGradeDetails(String name, @Nullable String labelColor) {

    private static final Pattern COLOR = Pattern.compile("#[0-9A-F]{6}");

    public QualityGradeDetails {
        name = MasterDataText.clean("name", name);
        labelColor = color(labelColor);
    }

    private static @Nullable String color(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String color = value.strip().toUpperCase(Locale.ROOT);
        if (!COLOR.matcher(color).matches()) {
            throw new InvalidMasterDataException("labelColor", "invalid color");
        }
        return color;
    }
}
