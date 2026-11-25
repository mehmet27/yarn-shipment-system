package com.yarnshipment.shipment.domain.fabric;

import java.io.Serial;
import java.util.UUID;

/** Kumaş reçetesi bulunamadı (404, {@code FABRIC_RECIPE_NOT_FOUND}). */
public final class FabricRecipeNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FabricRecipeNotFoundException(UUID id) {
        super("Fabric recipe not found id=" + id);
    }
}
