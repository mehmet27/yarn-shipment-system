package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.SealCondition;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentAction;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentStatus;
import com.yarnshipment.shipment.adapter.in.web.api.model.WeighingView;
import com.yarnshipment.shipment.domain.model.Weighing;
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

    static @Nullable SealCondition sealCondition(
            com.yarnshipment.shipment.domain.model.@Nullable SealCondition condition) {
        return condition == null ? null : SealCondition.valueOf(condition.name());
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
                .weighedAt(utc(weighing.weighedAt()));
    }
}
