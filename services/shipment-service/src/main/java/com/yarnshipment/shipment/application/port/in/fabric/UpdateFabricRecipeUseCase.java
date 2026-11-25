package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.FabricRecipe;

/** Karışımı değiştirir ya da pasife alır/yeniden açar; kimliği olan kayıt değişir. */
public interface UpdateFabricRecipeUseCase {

    FabricRecipe updateFabricRecipe(FabricRecipe recipe);
}
