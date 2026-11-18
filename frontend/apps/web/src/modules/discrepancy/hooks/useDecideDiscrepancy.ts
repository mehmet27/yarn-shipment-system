import type { ResolveDiscrepancyRequest } from '@iplik/api-client';
import {
  assignDiscrepancy,
  DISCREPANCIES_PATH,
  resolveDiscrepancy,
  SHIPMENTS_PATH,
} from '@iplik/api-client';
import { BALANCES_PATH, FIRE_SETTLEMENTS_PATH } from '@iplik/api-client/settlements';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Karar sonrası detay, geçmiş ve liste yeniden okunur (hepsi aynı anahtar önekinde). */
export function useRefreshDiscrepancies() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: [DISCREPANCIES_PATH] });
}

export function useAssignDiscrepancy(id: string) {
  const refresh = useRefreshDiscrepancies();
  return useMutation({
    mutationFn: (assigneeId: string) => assignDiscrepancy(id, { assigneeId }),
    onSuccess: refresh,
  });
}

/** Karar kaydedilir; haklı karar fire dosyasında faturalanacak kilo olarak görünür, bu yüzden ilgili listeler yenilenir. */
export function useResolveDiscrepancy(id: string, shipmentId: string) {
  const queryClient = useQueryClient();
  const refreshDiscrepancies = useRefreshDiscrepancies();
  return useMutation({
    mutationFn: (request: ResolveDiscrepancyRequest) => resolveDiscrepancy(id, request),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH, shipmentId] }),
        queryClient.invalidateQueries({ queryKey: [BALANCES_PATH] }),
        queryClient.invalidateQueries({ queryKey: [FIRE_SETTLEMENTS_PATH] }),
      ]);
      await refreshDiscrepancies();
    },
  });
}
