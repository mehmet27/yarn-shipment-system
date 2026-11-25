package com.yarnshipment.shipment.domain.fabric;

import java.io.Serial;

/** Bu kumaş cinsi için zaten bir reçete var (409, {@code FABRIC_RECIPE_EXISTS}). */
public final class FabricRecipeConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FabricRecipeConflictException(String fabricType) {
        super("Fabric recipe already exists fabricType=" + fabricType);
    }
}
