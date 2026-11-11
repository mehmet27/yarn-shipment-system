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
 * @param spec sipariş formu ayrıntıları; girilmediyse boş
 */
public record RecipeLine(
        int lineNo,
        @Nullable UUID yarnTypeId,
        @Nullable String yarnTypeText,
        @Nullable UUID lotId,
        BigDecimal netKg,
        @Nullable OrderSpec spec) {

    public static final int MAX_TEXT_LENGTH = 200;
    private static final int KG_SCALE = 3;

    /** Sipariş formu alanları olmayan satır. */
    public RecipeLine(
            int lineNo,
            @Nullable UUID yarnTypeId,
            @Nullable String yarnTypeText,
            @Nullable UUID lotId,
            BigDecimal netKg) {
        this(lineNo, yarnTypeId, yarnTypeText, lotId, netKg, null);
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
        yarnTypeText = text;
        netKg = netKg.setScale(KG_SCALE);
    }

    /** Sipariş formu ayrıntıları; girilmediyse alanları boş olan {@link OrderSpec#EMPTY}. */
    public OrderSpec orderSpec() {
        return spec == null ? OrderSpec.EMPTY : spec;
    }

    public RecipeLine withNetKg(BigDecimal newNetKg) {
        return new RecipeLine(lineNo, yarnTypeId, yarnTypeText, lotId, newNetKg, spec);
    }
}
