import { DISCREPANCIES_PATH, getDiscrepancy, listDiscrepancyActivity } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export function useDiscrepancy(id: string) {
  return useQuery({
    queryKey: [DISCREPANCIES_PATH, 'detail', id],
    queryFn: () => getDiscrepancy(id),
  });
}

export function useDiscrepancyActivity(id: string) {
  return useQuery({
    queryKey: [DISCREPANCIES_PATH, 'activity', id],
    queryFn: () => listDiscrepancyActivity(id),
  });
}
