package com.yarnshipment.shipment.application.port.in.weight;

/**
 * Müdür kantar uyuşmazlığındaki sevkiyatın reçetesini düzeltir. Reçete toplamı tartılan net'i tolerans içinde
 * tutuyorsa sevkiyat yola çıkar; tutmuyorsa {@code RecipeWeightMismatchException} fırlar ve hiçbir şey kaydedilmez.
 */
public interface UpdateRecipeUseCase {

    void updateRecipe(UpdateRecipeCommand command);
}
