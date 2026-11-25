package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kumaş reçetesi portu; kayıt silinmez, pasifleştirilir. */
public interface FabricRecipePort {

    /** Reçeteler kumaş cinsine göre; yalnız aktifler ya da hepsi. */
    List<FabricRecipe> findAll(FabricRecipeScope scope);

    /** Yeni reçete ekler; aynı kumaş cinsi varsa {@code FabricRecipeConflictException}. */
    void insert(FabricRecipe recipe, UUID createdBy, Instant at);

    /** Reçeteyi değiştirir; yoksa {@code FabricRecipeNotFoundException}, başka reçetenin cinsiyse çakışma. */
    void replace(FabricRecipe recipe, Instant at);
}
