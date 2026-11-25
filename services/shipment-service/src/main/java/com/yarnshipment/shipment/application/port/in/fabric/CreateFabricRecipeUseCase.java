package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import java.util.List;

/** Kumaş cinsi için yeni karışım tanımlar; aynı cins ikinci kez eklenemez. */
public interface CreateFabricRecipeUseCase {

    FabricRecipe createFabricRecipe(Actor actor, String fabricType, List<FabricRecipe.Line> yarns);
}
