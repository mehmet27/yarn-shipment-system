package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.util.List;

/** Kumaş cinsi karışımlarını (reçete) listeler. */
public interface ListFabricRecipesUseCase {

    List<FabricRecipe> listFabricRecipes(FabricRecipeScope scope);
}
