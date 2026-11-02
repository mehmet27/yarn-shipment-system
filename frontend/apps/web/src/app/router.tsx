import { RequireAuth, RequirePermission } from '@/modules/auth';
import { bagPanelRoutes } from '@/modules/bag';
import { discrepancyPanelRoutes } from '@/modules/discrepancy';
import { masterDataPanelRoutes } from '@/modules/masterdata';
import { shipmentFieldRoutes, shipmentPanelRoutes } from '@/modules/shipment';
import { createBrowserRouter } from 'react-router';
import type { RouteObject } from 'react-router';
import { FieldLayout } from './layouts/FieldLayout';
import { PanelLayout } from './layouts/PanelLayout';
import { RouteErrorPage } from './pages/RouteErrorPage';
import { ErrorPage } from './pages/ErrorPage';
import { HomeRedirectPage } from './pages/HomeRedirectPage';
import { PanelHomePage } from './pages/PanelHomePage';

/** Mimari doküman "Routing": /panel masaüstü, /saha mobil kabuk; her route bir izin taşır. */
const routes: RouteObject[] = [
  {
    path: '/',
    element: (
      <RequireAuth errorFallback={<ErrorPage reason="unexpected" />}>
        <HomeRedirectPage />
      </RequireAuth>
    ),
  },
  {
    path: '/panel',
    element: (
      <RequireAuth errorFallback={<ErrorPage reason="unexpected" />}>
        <RequirePermission permission="SHIPMENT_VIEW">
          <PanelLayout />
        </RequirePermission>
      </RequireAuth>
    ),
    children: [
      { index: true, element: <PanelHomePage /> },
      ...shipmentPanelRoutes,
      ...discrepancyPanelRoutes,
      ...masterDataPanelRoutes,
      ...bagPanelRoutes,
    ],
  },
  {
    path: '/saha',
    element: (
      <RequireAuth errorFallback={<ErrorPage reason="unexpected" />}>
        <FieldLayout />
      </RequireAuth>
    ),
    children: shipmentFieldRoutes,
  },
  { path: '/403', element: <ErrorPage reason="forbidden" /> },
  { path: '*', element: <ErrorPage reason="notFound" /> },
];

/** Her kök route bir hata sayfası taşır; alt sayfadaki çökme beyaz ekran bırakmaz (H-25). */
export const appRoutes: RouteObject[] = routes.map((route) => ({
  ...route,
  errorElement: <RouteErrorPage />,
}));

export function createAppRouter() {
  return createBrowserRouter(appRoutes);
}
