import type { CreateShipmentRequest } from '@iplik/api-client';
import { createShipment, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';

export function useCreateShipment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateShipmentRequest) => createShipment(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
