import type { GetDashboardParams } from '@iplik/api-client';
import {
  getDashboard,
  getShipmentReport,
  getSubcontractorBalanceReport,
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
      const [dashboard, subcontractors, shipments, balances] = await Promise.all([
        getDashboard(params),
        getSubcontractorReport(params),
        getShipmentReport(params),
        getSubcontractorBalanceReport(params),
      ]);
      return {
        from: dashboard.from,
        to: dashboard.to,
        dashboard,
        subcontractors,
        shipments,
        balances,
      };
    },
  };
}
