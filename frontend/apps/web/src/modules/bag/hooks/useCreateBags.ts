import type { CreateBagsRequest } from '@iplik/api-client';
import { createBags } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LOTS_QUERY_KEY } from './useLockedLots';

/** Üretimden sonra parti listesi (çuval sayısı) ve partinin çuvalları yeniden okunur. */
export function useCreateBags() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateBagsRequest) => createBags(request).then((r) => r.bags),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOTS_QUERY_KEY }),
  });
}
