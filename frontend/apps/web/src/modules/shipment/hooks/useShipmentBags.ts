import { getShipmentBags, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export function useShipmentBags(shipmentId: string) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, shipmentId, 'bags'],
    queryFn: () => getShipmentBags(shipmentId),
  });
}
