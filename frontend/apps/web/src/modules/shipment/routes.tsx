import type { RouteObject } from 'react-router';
import { ShipmentFilePage } from './pages/ShipmentFilePage';
import { ShipmentListPage } from './pages/ShipmentListPage';

/** /panel altındaki sevkiyat route'ları. */
export const shipmentPanelRoutes: RouteObject[] = [
  { path: 'sevkiyat', element: <ShipmentListPage /> },
  { path: 'sevkiyat/:shipmentId', element: <ShipmentFilePage /> },
];
