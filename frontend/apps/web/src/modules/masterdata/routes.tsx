import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { LotsPage } from './pages/LotsPage';
import { MasterDataPage } from './pages/MasterDataPage';

/** /panel altındaki yönetici ana veri route'ları. */
export const masterDataPanelRoutes: RouteObject[] = [
  {
    path: 'tanimlar',
    element: (
      <RequirePermission permission="MASTER_DATA_MANAGE">
        <MasterDataPage />
      </RequirePermission>
    ),
  },
  {
    path: 'partiler',
    element: (
      <RequirePermission permission="LOT_QUALITY_LOCK">
        <LotsPage />
      </RequirePermission>
    ),
  },
];
