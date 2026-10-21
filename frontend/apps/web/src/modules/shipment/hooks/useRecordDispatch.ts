import type { DispatchWeighingRequest } from '@iplik/api-client';
import { recordDispatchWeighing, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Başarıdan sonra sevkiyat yeniden okunur; yeni durum (Yüklendi) ve aksiyonlar backend'den gelir. */
export function useRecordDispatch(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: DispatchWeighingRequest) => recordDispatchWeighing(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
