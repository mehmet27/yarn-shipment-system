import type { WeightDecision } from '@iplik/api-client';
import { decideWeightMismatch, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Karardan sonra sevkiyat ve liste yeniden okunur; yeni durum backend'den gelir. */
export function useWeightDecision(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { decision: WeightDecision; reason: string }) =>
      decideWeightMismatch(shipmentId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
