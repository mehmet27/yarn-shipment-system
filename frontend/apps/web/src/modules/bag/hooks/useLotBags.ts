import { listLotBags } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { LOTS_QUERY_KEY } from './useLockedLots';

export function useLotBags(lotId: string) {
  return useQuery({
    queryKey: [...LOTS_QUERY_KEY, lotId, 'bags'],
    queryFn: () => listLotBags(lotId).then((r) => r.bags),
  });
}
