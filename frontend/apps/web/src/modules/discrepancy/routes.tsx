import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { DiscrepancyDetailPage } from './pages/DiscrepancyDetailPage';
import { DiscrepancyListPage } from './pages/DiscrepancyListPage';

/** /panel altındaki uyuşmazlık route'ları. */
export const discrepancyPanelRoutes: RouteObject[] = [
  {
    path: 'uyusmazliklar',
    element: (
      <RequirePermission permission="DISCREPANCY_VIEW">
        <DiscrepancyListPage />
      </RequirePermission>
    ),
  },
  {
    path: 'uyusmazliklar/:discrepancyId',
    element: (
      <RequirePermission permission="DISCREPANCY_VIEW">
        <DiscrepancyDetailPage />
      </RequirePermission>
    ),
  },
];
