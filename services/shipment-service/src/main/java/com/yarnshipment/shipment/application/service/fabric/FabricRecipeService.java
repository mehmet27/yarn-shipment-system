package com.yarnshipment.shipment.application.service.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.application.port.in.fabric.CreateFabricRecipeUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricRecipesUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.UpdateFabricRecipeUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.FabricRecipePort;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kumaş cinsi reçetelerini listeler, ekler ve değiştirir; yetki denetimi uçta yapılır (MASTER_DATA_MANAGE). */
@Service
public class FabricRecipeService
        implements ListFabricRecipesUseCase, CreateFabricRecipeUseCase, UpdateFabricRecipeUseCase {

    private final FabricRecipePort recipes;
    private final Clock clock;

    public FabricRecipeService(FabricRecipePort recipes, Clock clock) {
        this.recipes = recipes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricRecipe> listFabricRecipes(FabricRecipeScope scope) {
        return recipes.findAll(scope);
    }

    @Override
    @Transactional
    public FabricRecipe createFabricRecipe(Actor actor, String fabricType, List<FabricRecipe.Line> yarns) {
        FabricRecipe recipe = new FabricRecipe(UUID.randomUUID(), fabricType, yarns, true);
        recipes.insert(recipe, actor.userId(), clock.instant());
        return recipe;
    }

    @Override
    @Transactional
    public FabricRecipe updateFabricRecipe(FabricRecipe recipe) {
        recipes.replace(recipe, clock.instant());
        return recipe;
    }
}
