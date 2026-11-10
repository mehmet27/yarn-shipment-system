package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.RecipeLineView;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentAction;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentStatus;
import com.yarnshipment.shipment.adapter.in.web.api.model.WeighingView;
import com.yarnshipment.shipment.adapter.in.web.api.model.WeightCheckView;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.Weighing;
import com.yarnshipment.shipment.domain.model.WeightCheck;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Domain değerlerinin sözleşmedeki karşılıkları (API-03). */
final class WebMappings {

    private static final int KG_SCALE = 3;

    private WebMappings() {}

    /** Kilo dize olarak, üç ondalıkla: {@code "1982.500"}. */
    static @Nullable String pct(@Nullable BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Fire oranı dize olarak, dört ondalıkla: {@code "0.0400"}. */
    static String fireRate(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    static List<RecipeLineView> lines(List<RecipeLine> lines) {
        return lines.stream()
                .map(line -> new RecipeLineView()
                        .lineNo(line.lineNo())
                        .yarnTypeId(line.yarnTypeId())
                        .yarnTypeText(line.yarnTypeText())
                        .lotId(line.lotId())
                        .netKg(kg(line.netKg())))
                .toList();
    }

    static @Nullable WeightCheckView weightCheck(@Nullable WeightCheck check) {
        if (check == null) {
            return null;
        }
        return new WeightCheckView()
                .expectedKg(kg(check.expectedKg()))
                .measuredKg(kg(check.measuredKg()))
                .diffKg(check.diffKg()
                        .setScale(KG_SCALE, RoundingMode.UNNECESSARY)
                        .toPlainString())
                .shortfallReason(check.shortfallReason());
    }

    static String kg(BigDecimal value) {
        return value.setScale(KG_SCALE, RoundingMode.UNNECESSARY).toPlainString();
    }

    static @Nullable String kgOrNull(@Nullable BigDecimal value) {
        return value == null ? null : kg(value);
    }

    static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    static ShipmentStatus status(com.yarnshipment.shipment.domain.model.ShipmentStatus status) {
        return ShipmentStatus.valueOf(status.name());
    }

    static List<ShipmentAction> actions(
            Collection<com.yarnshipment.shipment.application.port.in.ShipmentAction> actions) {
        return actions.stream()
                .sorted()
                .map(action -> ShipmentAction.valueOf(action.name()))
                .toList();
    }

    static @Nullable WeighingView weighing(@Nullable Weighing weighing) {
        if (weighing == null) {
            return null;
        }
        return new WeighingView()
                .grossKg(kg(weighing.grossKg()))
                .tareKg(kg(weighing.tareKg()))
                .netKg(kg(weighing.netKg()))
                .bagCount(weighing.bagCount())
                .weighedAt(utc(weighing.weighedAt()))
                .expectedTareKg(weighing.expectedTareKg() == null ? null : kg(weighing.expectedTareKg()))
                .tareTolerancePct(pct(weighing.tareTolerancePct()))
                .tareDeviationPct(pct(weighing.tareDeviationPct()))
                .tareAlert(weighing.tareAlert());
    }
}
