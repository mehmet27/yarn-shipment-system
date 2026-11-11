package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Sipariş formu (FASON SİPARİŞ FORMU) satır ayrıntıları; hepsi isteğe bağlıdır.
 *
 * @param fabricType kumaş cinsi
 * @param lycra LYC (ör. 30 denye)
 * @param widthType tüp / may / açık en
 * @param yarnLength iplik uzunluğu / lycra
 * @param pussFine puss-fine
 * @param unitPrice birim fiyat (TL/kg), en çok 4 ondalık
 */
public record OrderSpec(
        @Nullable String fabricType,
        @Nullable String lycra,
        @Nullable WidthType widthType,
        @Nullable String yarnLength,
        @Nullable String pussFine,
        @Nullable BigDecimal unitPrice) {

    /** Hiçbir alanı girilmemiş sipariş ayrıntısı. */
    public static final OrderSpec EMPTY = new OrderSpec(null, null, null, null, null, null);

    private static final int PRICE_SCALE = 4;

    public OrderSpec {
        fabricType = optionalText(fabricType, "fabricType");
        lycra = optionalText(lycra, "lycra");
        yarnLength = optionalText(yarnLength, "yarnLength");
        pussFine = optionalText(pussFine, "pussFine");
        unitPrice = price(unitPrice);
    }

    private static @Nullable BigDecimal price(@Nullable BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0 || value.stripTrailingZeros().scale() > PRICE_SCALE) {
            throw new InvalidNewShipmentException("lines", "unit price invalid");
        }
        return value.setScale(PRICE_SCALE);
    }

    private static @Nullable String optionalText(@Nullable String value, String field) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > RecipeLine.MAX_TEXT_LENGTH) {
            throw new InvalidNewShipmentException("lines", field + " too long");
        }
        return text;
    }
}
