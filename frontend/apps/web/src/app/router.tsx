import { RequireAuth, RequirePermission } from '@/modules/auth';
import { createBrowserRouter } from 'react-router';
import type { RouteObject } from 'react-router';
import { FieldLayout } from './layouts/FieldLayout';
import { PanelLayout } from './layouts/PanelLayout';
import { ErrorPage } from './pages/ErrorPage';
import { FieldHomePage } from './pages/FieldHomePage';
import { HomeRedirectPage } from './pages/HomeRedirectPage';
import { PanelHomePage } from './pages/PanelHomePage';

/** Mimari doküman "Routing": /panel masaüstü, /saha mobil kabuk; her route bir izin taşır. */
export const appRoutes: RouteObject[] = [
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
        <RequirePermission permission="SEVKIYAT_GORUNTULE">
          <PanelLayout />
        </RequirePermission>
      </RequireAuth>
    ),
    children: [{ index: true, element: <PanelHomePage /> }],
  },
  {
    path: '/saha',
    element: (
      <RequireAuth errorFallback={<ErrorPage reason="unexpected" />}>
        <FieldLayout />
      </RequireAuth>
    ),
    children: [{ index: true, element: <FieldHomePage /> }],
  },
  { path: '/403', element: <ErrorPage reason="forbidden" /> },
  { path: '*', element: <ErrorPage reason="notFound" /> },
];

export function createAppRouter() {
  return createBrowserRouter(appRoutes);
}
