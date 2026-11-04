import type { GetDashboardParams } from '@iplik/api-client';

export type ReportPanelProps = {
  params: GetDashboardParams;
  isEnabled: boolean;
  /** Yalnızca uyuşmazlıklı satırları göster */
  isOnlyDiscrepant?: boolean;
};
