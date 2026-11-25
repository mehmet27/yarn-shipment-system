package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.adapter.in.web.ledger.FabricViews.YarnShareView;
import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.application.port.in.fabric.FabricYarnOption;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;
import com.yarnshipment.shipment.domain.fabric.YarnShortage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Kumaş girişinin düşüm önizlemesi, fasoncunun elindeki iplikler ve sevkiyatın ipliğinden üretilen kumaş yanıtları. */
final class FabricPlanViews {

    private FabricPlanViews() {}

    record ShortageView(
            String yarnType, @Nullable UUID lotId, @Nullable String lotNo, String requiredKg, String missingKg) {}

    record FabricPreviewView(boolean feasible, List<YarnShareView> yarns, List<ShortageView> shortages) {}

    record YarnOptionView(
            String yarnType,
            @Nullable UUID lotId,
            @Nullable String lotNo,
            @Nullable String supplierName,
            String totalKg,
            String ownKg) {}

    record FabricDrawView(
            UUID fabricId,
            String fabricType,
            UUID entryShipmentId,
            String entryShipmentNo,
            String yarnType,
            @Nullable String lotNo,
            String netKg,
            Instant receivedAt) {}

    record RecipeLineView(String yarnType, String ratioPct) {}

    record FabricRecipeView(UUID id, String fabricType, List<RecipeLineView> yarns, boolean active) {}

    static FabricRecipeView recipe(FabricRecipe recipe) {
        return new FabricRecipeView(
                recipe.id(),
                recipe.fabricType(),
                recipe.yarns().stream()
                        .map(line -> new RecipeLineView(
                                line.yarnType(), line.ratioPct().toPlainString()))
                        .toList(),
                recipe.isActive());
    }

    static FabricPreviewView preview(Plan plan) {
        return new FabricPreviewView(
                plan.isFeasible(),
                plan.yarns().stream().map(FabricViews::yarn).toList(),
                plan.shortages().stream().map(FabricPlanViews::shortage).toList());
    }

    private static ShortageView shortage(YarnShortage shortage) {
        return new ShortageView(
                shortage.yarnType(),
                shortage.lotId(),
                shortage.lotNo(),
                BalanceViews.kg(shortage.requiredKg()),
                BalanceViews.kg(shortage.missingKg()));
    }

    static YarnOptionView option(FabricYarnOption option) {
        return new YarnOptionView(
                option.yarnType(),
                option.lotId(),
                option.lotNo(),
                option.supplierName(),
                BalanceViews.kg(option.totalKg()),
                BalanceViews.kg(option.ownKg()));
    }

    static FabricDrawView drawEntry(FabricDrawEntry entry) {
        return new FabricDrawView(
                entry.fabricId(),
                entry.fabricType(),
                entry.entryShipmentId(),
                entry.entryShipmentNo(),
                entry.yarnType(),
                entry.lotNo(),
                BalanceViews.kg(entry.netKg()),
                entry.receivedAt());
    }
}
