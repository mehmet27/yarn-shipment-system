import type { GetDashboardParams } from '@iplik/api-client';
import { getDashboard, REPORTS_PATH } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export function useDashboard(params: GetDashboardParams) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'dashboard', params],
    queryFn: () => getDashboard(params),
    placeholderData: keepPreviousData,
  });
}
