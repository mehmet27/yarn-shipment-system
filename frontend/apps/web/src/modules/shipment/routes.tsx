import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { CreateShipmentPage } from './pages/CreateShipmentPage';
import { ExitSlipPage } from './pages/ExitSlipPage';
import { DispatchWeighingPage } from './pages/DispatchWeighingPage';
import { ShipmentFilePage } from './pages/ShipmentFilePage';
import { ShipmentListPage } from './pages/ShipmentListPage';

/** /panel altındaki sevkiyat route'ları. */
export const shipmentPanelRoutes: RouteObject[] = [
  { path: 'sevkiyat', element: <ShipmentListPage /> },
  {
    path: 'sevkiyat/yeni',
    element: (
      <RequirePermission permission="SHIPMENT_CREATE">
        <CreateShipmentPage />
      </RequirePermission>
    ),
  },
  { path: 'sevkiyat/:shipmentId', element: <ShipmentFilePage /> },
  { path: 'sevkiyat/:shipmentId/ambar-cikis-fisi', element: <ExitSlipPage /> },
  {
    path: 'sevkiyat/:shipmentId/cikis',
    element: (
      <RequirePermission permission="DISPATCH_RECORD">
        <DispatchWeighingPage />
      </RequirePermission>
    ),
  },
];
