import type { RouteObject } from 'react-router';
import { FieldShipmentListPage } from './pages/FieldShipmentListPage';
import { FieldShipmentPage } from './pages/FieldShipmentPage';
import { ShipmentFilePage } from './pages/ShipmentFilePage';
import { ShipmentListPage } from './pages/ShipmentListPage';

/** /panel altındaki sevkiyat route'ları. */
export const shipmentPanelRoutes: RouteObject[] = [
  { path: 'sevkiyat', element: <ShipmentListPage /> },
  { path: 'sevkiyat/:shipmentId', element: <ShipmentFilePage /> },
];

/** /saha altındaki sevkiyat route'ları (mobil). */
export const shipmentFieldRoutes: RouteObject[] = [
  { index: true, element: <FieldShipmentListPage /> },
  { path: 'sevkiyat/:shipmentId', element: <FieldShipmentPage /> },
];
