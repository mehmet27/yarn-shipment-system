import type { ListShipmentsParams } from '@iplik/api-client';
import { listShipments, SHIPMENTS_PATH } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export function useShipments(params: ListShipmentsParams) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, params],
    queryFn: () => listShipments(params),
    placeholderData: keepPreviousData,
  });
}
