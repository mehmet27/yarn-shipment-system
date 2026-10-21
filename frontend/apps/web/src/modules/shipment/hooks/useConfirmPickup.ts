import { confirmPickup, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Başarılı beyandan sonra sevkiyat yeniden okunur; yeni durum ve aksiyonlar backend'den gelir. */
export function useConfirmPickup(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => confirmPickup(shipmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
