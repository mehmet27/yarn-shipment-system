import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { LoginAccountsPage } from './pages/LoginAccountsPage';

/** /panel altındaki kullanıcı yönetimi route'ları; yalnızca USER_MANAGE (yönetici). */
export const usersPanelRoutes: RouteObject[] = [
  {
    path: 'kullanicilar',
    element: (
      <RequirePermission permission="USER_MANAGE">
        <LoginAccountsPage />
      </RequirePermission>
    ),
  },
];
