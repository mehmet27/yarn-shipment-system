import type { AssignShipmentBagsRequest } from '@iplik/api-client';
import { assignShipmentBags, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Atamadan sonra sevkiyat ve çuval listesi yeniden okunur (çuval listesi anahtarı aynı önekte). */
export function useAssignBags(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AssignShipmentBagsRequest) => assignShipmentBags(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
