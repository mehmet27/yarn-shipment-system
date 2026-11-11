import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { CentralStockPage } from './pages/CentralStockPage';

/** /panel altındaki merkez iplik stoku route'u. */
export const stockPanelRoutes: RouteObject[] = [
  {
    path: 'merkez-stok',
    element: (
      <RequirePermission permission="CENTRAL_STOCK_VIEW">
        <CentralStockPage />
      </RequirePermission>
    ),
  },
];
