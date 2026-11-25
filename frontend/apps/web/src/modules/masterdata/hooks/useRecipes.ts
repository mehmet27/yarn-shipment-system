import {
  createFabricRecipe,
  FABRIC_RECIPES_PATH,
  listFabricRecipes,
  updateFabricRecipe,
} from '@iplik/api-client/fabric';
import type { FabricRecipeRequest } from '@iplik/api-client/fabric';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

/** Yönetici ekranı pasif karışımları da listeler. */
export const useAllFabricRecipes = () =>
  useQuery({
    queryKey: [FABRIC_RECIPES_PATH, 'all'],
    queryFn: () => listFabricRecipes(true),
  });

export function useSaveFabricRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: string | undefined; request: FabricRecipeRequest }) =>
      id === undefined ? createFabricRecipe(request) : updateFabricRecipe(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [FABRIC_RECIPES_PATH] }),
  });
}
