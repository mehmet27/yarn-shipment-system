import { removeShipmentBag, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Çıkarmadan sonra çuval listesi yeniden okunur. */
export function useRemoveBag(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (uniqueKey: string) => removeShipmentBag(shipmentId, uniqueKey),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
