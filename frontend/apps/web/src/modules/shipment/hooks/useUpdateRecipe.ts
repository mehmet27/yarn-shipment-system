import type { UpdateRecipeRequest } from '@iplik/api-client';
import { SHIPMENTS_PATH, updateRecipeAndDispatch } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Reçete tartıyı tutarsa sevkiyat yola çıkar; sevkiyat ve liste yeniden okunur. */
export function useUpdateRecipe(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateRecipeRequest) => updateRecipeAndDispatch(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
