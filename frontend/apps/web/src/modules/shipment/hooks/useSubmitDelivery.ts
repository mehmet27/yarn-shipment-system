import type { DeliveryReceiptRequest } from '@iplik/api-client';
import { SHIPMENTS_PATH, submitDeliveryReceipt } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

/** Aynı mutasyon nesnesi tekrar denense de Idempotency-Key çift tutanağı önler (backend 204 döner). */
export function useSubmitDelivery(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: DeliveryReceiptRequest) => submitDeliveryReceipt(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
