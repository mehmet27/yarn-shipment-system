import type { ResolveDiscrepancyRequest } from '@iplik/api-client';
import { assignDiscrepancy, DISCREPANCIES_PATH, resolveDiscrepancy } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Karar sonrası detay, geçmiş ve liste yeniden okunur (hepsi aynı anahtar önekinde). */
function useRefresh() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: [DISCREPANCIES_PATH] });
}

export function useAssignDiscrepancy(id: string) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (assigneeId: string) => assignDiscrepancy(id, { assigneeId }),
    onSuccess: refresh,
  });
}

export function useResolveDiscrepancy(id: string) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (request: ResolveDiscrepancyRequest) => resolveDiscrepancy(id, request),
    onSuccess: refresh,
  });
}
