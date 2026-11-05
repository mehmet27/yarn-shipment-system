import type { VoidBagsRequest } from '@iplik/api-client';
import { voidBags } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LOTS_QUERY_KEY } from './useLockedLots';

/** İptalden sonra parti listesi (çuval sayısı) ve çuvalların durumu yeniden okunur. */
export function useVoidBags() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: VoidBagsRequest) => voidBags(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOTS_QUERY_KEY }),
  });
}
