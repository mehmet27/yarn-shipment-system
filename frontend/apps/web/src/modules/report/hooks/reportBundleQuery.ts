import type { GetDashboardParams } from '@iplik/api-client';
import {
  getDashboard,
  getDriverReport,
  getPairReport,
  getSubcontractorReport,
  REPORTS_PATH,
} from '@iplik/api-client';
import type { ReportBundle } from '../export/reportModel';

/** Dışa aktarma için tüm raporlar aynı dönemle çekilir; ekrandaki sekmeden bağımsızdır. */
export function reportBundleQuery(params: GetDashboardParams) {
  return {
    queryKey: [REPORTS_PATH, 'bundle', params],
    staleTime: 0,
    queryFn: async (): Promise<ReportBundle> => {
      const [dashboard, drivers, subcontractors, pairs] = await Promise.all([
        getDashboard(params),
        getDriverReport(params),
        getSubcontractorReport(params),
        getPairReport(params),
      ]);
      return { from: dashboard.from, to: dashboard.to, dashboard, drivers, subcontractors, pairs };
    },
  };
}
