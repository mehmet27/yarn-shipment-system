import { RequirePermission } from '@/modules/auth';
import { Navigate } from 'react-router';
import type { RouteObject } from 'react-router';
import { BalancesPage } from './pages/BalancesPage';
import { InvoicingPage } from './pages/InvoicingPage';
import { SubcontractorPage } from './pages/SubcontractorPage';

/** /panel altındaki fasoncu bakiyesi ve faturalama route'ları. */
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
    path: 'bakiye/fasoncu/:subcontractorId',
    element: (
      <RequirePermission permission="BALANCE_VIEW">
        <SubcontractorPage />
      </RequirePermission>
    ),
  },
  {
    path: 'faturalama',
    element: (
      <RequirePermission permission="INVOICE_MARK">
        <InvoicingPage />
      </RequirePermission>
    ),
  },
  /** Ekranın eski adı: kayıtlı bağlantılar ve eski metinler yeni ekrana gider. */
  { path: 'faturalanacaklar', element: <Navigate to="/panel/faturalama" replace /> },
];
