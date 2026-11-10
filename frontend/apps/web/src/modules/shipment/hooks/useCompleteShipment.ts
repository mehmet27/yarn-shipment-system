import type { CompleteShipmentRequest } from '@iplik/api-client';
import { completeShipment, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** İrsaliyeyle tamamlamadan sonra sevkiyat ve liste yeniden okunur. */
export function useCompleteShipment(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CompleteShipmentRequest) => completeShipment(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
