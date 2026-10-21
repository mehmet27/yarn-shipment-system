import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { CreateShipmentPage } from './pages/CreateShipmentPage';
import { DeliveryReceiptPage } from './pages/DeliveryReceiptPage';
import { DispatchWeighingPage } from './pages/DispatchWeighingPage';
import { FieldShipmentListPage } from './pages/FieldShipmentListPage';
import { FieldShipmentPage } from './pages/FieldShipmentPage';
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
  {
    path: 'sevkiyat/:shipmentId/cikis',
    element: (
      <RequirePermission permission="DISPATCH_RECORD">
        <DispatchWeighingPage />
      </RequirePermission>
    ),
  },
];

/** /saha altındaki sevkiyat route'ları (mobil). */
export const shipmentFieldRoutes: RouteObject[] = [
  { index: true, element: <FieldShipmentListPage /> },
  { path: 'sevkiyat/:shipmentId', element: <FieldShipmentPage /> },
  { path: 'sevkiyat/:shipmentId/teslim', element: <DeliveryReceiptPage /> },
];
