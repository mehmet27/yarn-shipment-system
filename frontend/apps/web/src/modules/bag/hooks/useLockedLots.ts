import { BAGS_PATH, listLockedLots } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export const LOTS_QUERY_KEY = [BAGS_PATH, 'lots'] as const;

export function useLockedLots() {
  return useQuery({
    queryKey: LOTS_QUERY_KEY,
    queryFn: () => listLockedLots().then((r) => r.lots),
  });
}
