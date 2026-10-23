import { cancelShipment, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** İptalden sonra sevkiyat, liste ve çuvallar yeniden okunur. */
export function useCancelShipment(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => cancelShipment(shipmentId, { reason }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
