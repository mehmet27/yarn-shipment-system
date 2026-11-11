import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { BalancesPage } from './pages/BalancesPage';
import { ToInvoicePage } from './pages/ToInvoicePage';

/** /panel altındaki fasoncu bakiyesi ve faturalanacaklar route'ları. */
export const balancePanelRoutes: RouteObject[] = [
  {
    path: 'bakiye',
    element: (
      <RequirePermission permission="BALANCE_VIEW">
        <BalancesPage />
      </RequirePermission>
    ),
  },
  {
    path: 'faturalanacaklar',
    element: (
      <RequirePermission permission="INVOICE_MARK">
        <ToInvoicePage />
      </RequirePermission>
    ),
  },
];
