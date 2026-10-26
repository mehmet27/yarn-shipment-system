import { declareArrival, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Başarılı beyandan sonra sevkiyat yeniden okunur; yeni durum ve aksiyonlar backend'den gelir. */
export function useDeclareArrival(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sealIntact: boolean) => declareArrival(shipmentId, { sealIntact }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
