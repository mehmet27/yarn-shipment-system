import type { GetDashboardParams } from '@iplik/api-client';
import { getDashboard, REPORTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { previousPeriod } from '../comparison';

/**
 * Seçilen dönemin hemen öncesindeki aynı uzunluktaki dönemin özeti; kartlardaki "önceki dönem" kıyası için.
 * Dönem sınırı yoksa çekilmez; hata kıyası gizler, ekranı bozmaz.
 */
export function usePreviousDashboard(params: GetDashboardParams) {
  const { from, to, ...rest } = params;
  const hasPeriod = from !== undefined && to !== undefined;
  const previous = hasPeriod ? previousPeriod({ from, to }) : undefined;
  return useQuery({
    queryKey: [REPORTS_PATH, 'dashboard', { ...rest, ...previous }],
    queryFn: () => getDashboard({ ...rest, ...previous }),
    enabled: hasPeriod,
  }).data;
}
