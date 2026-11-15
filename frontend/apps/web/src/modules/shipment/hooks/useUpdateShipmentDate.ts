import { SHIPMENTS_PATH, updateShipmentDate } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Gün güncellenince sevkiyat dosyası ve liste yeniden okunur. */
export function useUpdateShipmentDate(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (shipmentDate: string) => updateShipmentDate(shipmentId, { shipmentDate }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
