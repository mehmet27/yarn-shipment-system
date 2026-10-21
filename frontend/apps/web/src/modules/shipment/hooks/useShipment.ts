import { getShipment, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export function useShipment(shipmentId: string) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, shipmentId],
    queryFn: () => getShipment(shipmentId),
  });
}
