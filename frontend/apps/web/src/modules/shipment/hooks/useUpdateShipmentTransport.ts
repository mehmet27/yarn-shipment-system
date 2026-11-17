import type { UpdateShipmentTransportRequest } from '@iplik/api-client';
import { SHIPMENTS_PATH, updateShipmentTransport } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Şoför/araç düzeltilince sevkiyat dosyası ve liste yeniden okunur. */
export function useUpdateShipmentTransport(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateShipmentTransportRequest) =>
      updateShipmentTransport(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
