package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.ledger.FabricPlanViews.FabricRecipeView;
import com.yarnshipment.shipment.adapter.in.web.ledger.LedgerRequests.FabricRecipeRequest;
import com.yarnshipment.shipment.application.port.in.fabric.CreateFabricRecipeUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ListFabricRecipesUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.UpdateFabricRecipeUseCase;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kumaş cinsi için varsayılan iplik karışımı (reçete). Kumaş giren kaliteci aktifleri okur; ekleme ve değiştirme
 * yalnız MASTER_DATA_MANAGE. Method security proxy'si gerektirdiği için final değildir.
 */
@RestController
@RequestMapping("/api/v1/fabric-recipes")
public class FabricRecipeController {

    private final ListFabricRecipesUseCase listRecipes;
    private final CreateFabricRecipeUseCase createRecipe;
    private final UpdateFabricRecipeUseCase updateRecipe;
    private final CurrentActorProvider currentActorProvider;

    public FabricRecipeController(
            ListFabricRecipesUseCase listRecipes,
            CreateFabricRecipeUseCase createRecipe,
            UpdateFabricRecipeUseCase updateRecipe,
            CurrentActorProvider currentActorProvider) {
        this.listRecipes = listRecipes;
        this.createRecipe = createRecipe;
        this.updateRecipe = updateRecipe;
        this.currentActorProvider = currentActorProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MASTER_DATA_VIEW', 'FABRIC_RECEIPT_RECORD')")
    public ResponseEntity<List<FabricRecipeView>> listRecipes(
            @RequestParam(name = "scope", defaultValue = "ACTIVE") FabricRecipeScope scope) {
        return ResponseEntity.ok(listRecipes.listFabricRecipes(scope).stream()
                .map(FabricPlanViews::recipe)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<FabricRecipeView> createRecipe(@RequestBody FabricRecipeRequest request) {
        FabricRecipe created = createRecipe.createFabricRecipe(
                currentActorProvider.getCurrentActor(),
                LedgerRequests.required(request.fabricType(), "fabricType"),
                LedgerRequests.recipeLines(request.yarns()));
        return ResponseEntity.status(HttpStatus.CREATED).body(FabricPlanViews.recipe(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<FabricRecipeView> updateRecipe(
            @PathVariable UUID id, @RequestBody FabricRecipeRequest request) {
        FabricRecipe updated = updateRecipe.updateFabricRecipe(new FabricRecipe(
                id,
                LedgerRequests.required(request.fabricType(), "fabricType"),
                LedgerRequests.recipeLines(request.yarns()),
                request.active() == null || request.active()));
        return ResponseEntity.ok(FabricPlanViews.recipe(updated));
    }
}
