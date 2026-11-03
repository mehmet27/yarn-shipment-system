import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { ReportsPage } from './pages/ReportsPage';

/** /panel altındaki rapor route'ları. */
export const reportPanelRoutes: RouteObject[] = [
  {
    path: 'raporlar',
    element: (
      <RequirePermission permission="REPORT_VIEW">
        <ReportsPage />
      </RequirePermission>
    ),
  },
];
