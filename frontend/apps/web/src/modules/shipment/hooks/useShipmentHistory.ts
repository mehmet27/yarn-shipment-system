import { getShipmentHistory, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export function useShipmentHistory(shipmentId: string) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, shipmentId, 'history'],
    queryFn: () => getShipmentHistory(shipmentId),
  });
}
