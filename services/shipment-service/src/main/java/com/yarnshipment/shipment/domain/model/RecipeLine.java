package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Reçete satırı (süreç analizi 5.2): iplik cinsi listeden ({@code yarnTypeId}) ya da serbest metin, isteğe bağlı lot ve
 * net kilo. En az biri cins olarak verilir.
 *
 * @param lineNo satır sırası, 1'den başlar
 * @param yarnTypeId listeden seçilen iplik cinsi
 * @param yarnTypeText serbest metinle yazılan iplik cinsi
 * @param lotId isteğe bağlı lot
 * @param netKg net kilo, sıfırdan büyük, en çok 3 ondalık
 * @param fabricType sipariş formu: kumaş cinsi
 * @param lycra sipariş formu: LYC
 * @param widthType sipariş formu: tüp / may / açık en
 * @param yarnLength sipariş formu: iplik uzunluğu / lycra
 * @param pussFine sipariş formu: puss-fine
 * @param unitPrice sipariş formu: birim fiyat (TL/kg), en çok 4 ondalık
 */
public record RecipeLine(
        int lineNo,
        @Nullable UUID yarnTypeId,
        @Nullable String yarnTypeText,
        @Nullable UUID lotId,
        BigDecimal netKg,
        @Nullable String fabricType,
        @Nullable String lycra,
        @Nullable WidthType widthType,
        @Nullable String yarnLength,
        @Nullable String pussFine,
        @Nullable BigDecimal unitPrice) {

    public static final int MAX_TEXT_LENGTH = 200;
    private static final int KG_SCALE = 3;

    private static final int PRICE_SCALE = 4;

    /** Sipariş formu alanları olmayan satır. */
    public RecipeLine(
            int lineNo,
            @Nullable UUID yarnTypeId,
            @Nullable String yarnTypeText,
            @Nullable UUID lotId,
            BigDecimal netKg) {
        this(lineNo, yarnTypeId, yarnTypeText, lotId, netKg, null, null, null, null, null, null);
    }

    public RecipeLine {
        Objects.requireNonNull(netKg, "netKg");
        if (lineNo <= 0) {
            throw new InvalidNewShipmentException("lines", "line number not positive");
        }
        String text = yarnTypeText == null ? null : yarnTypeText.strip();
        if (text != null && (text.isEmpty() || text.length() > MAX_TEXT_LENGTH)) {
            throw new InvalidNewShipmentException("lines", "yarn type text invalid");
        }
        if (yarnTypeId == null && text == null) {
            throw new InvalidNewShipmentException("lines", "yarn type required");
        }
        if (netKg.signum() <= 0 || netKg.stripTrailingZeros().scale() > KG_SCALE) {
            throw new InvalidNewShipmentException("lines", "net kg must be positive with at most 3 decimals");
        }
        fabricType = optionalText(fabricType, "fabricType");
        lycra = optionalText(lycra, "lycra");
        yarnLength = optionalText(yarnLength, "yarnLength");
        pussFine = optionalText(pussFine, "pussFine");
        if (unitPrice != null) {
            if (unitPrice.signum() < 0 || unitPrice.stripTrailingZeros().scale() > PRICE_SCALE) {
                throw new InvalidNewShipmentException("lines", "unit price invalid");
            }
            unitPrice = unitPrice.setScale(PRICE_SCALE);
        }
        yarnTypeText = text;
        netKg = netKg.setScale(KG_SCALE);
    }

    public RecipeLine withNetKg(BigDecimal newNetKg) {
        return new RecipeLine(
                lineNo,
                yarnTypeId,
                yarnTypeText,
                lotId,
                newNetKg,
                fabricType,
                lycra,
                widthType,
                yarnLength,
                pussFine,
                unitPrice);
    }

    private static @Nullable String optionalText(@Nullable String value, String field) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidNewShipmentException("lines", field + " too long");
        }
        return text;
    }
}
