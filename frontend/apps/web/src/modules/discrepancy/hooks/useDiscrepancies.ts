import type { ListDiscrepanciesParams } from '@iplik/api-client';
import { DISCREPANCIES_PATH, listDiscrepancies } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export function useDiscrepancies(params: ListDiscrepanciesParams) {
  return useQuery({
    queryKey: [DISCREPANCIES_PATH, 'list', params],
    queryFn: () => listDiscrepancies(params),
    placeholderData: keepPreviousData,
  });
}
