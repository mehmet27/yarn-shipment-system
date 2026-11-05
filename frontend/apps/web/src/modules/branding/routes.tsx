import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { BrandingSettingsPage } from './pages/BrandingSettingsPage';

/** /panel altındaki görünüm (marka) route'u. */
export const brandingPanelRoutes: RouteObject[] = [
  {
    path: 'gorunum',
    element: (
      <RequirePermission permission="SETTINGS_MANAGE">
        <BrandingSettingsPage />
      </RequirePermission>
    ),
  },
];
