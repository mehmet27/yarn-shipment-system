import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { RolePermissionsPage } from './pages/RolePermissionsPage';

/** /panel altındaki yetki yönetimi route'u; yalnızca PERMISSION_MANAGE (yönetici). */
export const rolePermissionsPanelRoutes: RouteObject[] = [
  {
    path: 'yetkiler',
    element: (
      <RequirePermission permission="PERMISSION_MANAGE">
        <RolePermissionsPage />
      </RequirePermission>
    ),
  },
];
