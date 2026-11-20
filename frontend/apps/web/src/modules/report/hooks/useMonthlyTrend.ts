import type { GetDashboardParams } from '@iplik/api-client';
import { getMonthlyTrend, REPORTS_PATH } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export const TREND_MONTHS = 6;

/** Seçilen dönemin bitiş ayında sonlanan son altı ayın eğilimi; dönemin başlangıcı eğilimi etkilemez. */
export function useMonthlyTrend(params: GetDashboardParams) {
  const query = {
    months: TREND_MONTHS,
    ...(params.to === undefined ? {} : { to: params.to }),
    ...(params.siteId === undefined ? {} : { siteId: params.siteId }),
  };
  return useQuery({
    queryKey: [REPORTS_PATH, 'monthly-trend', query],
    queryFn: () => getMonthlyTrend(query),
    placeholderData: keepPreviousData,
  });
}
