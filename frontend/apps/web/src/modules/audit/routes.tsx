import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { AuditLogPage } from './pages/AuditLogPage';

/** /panel altındaki denetim kaydı route'u; yalnızca AUDIT_VIEW. */
export const auditPanelRoutes: RouteObject[] = [
  {
    path: 'denetim',
    element: (
      <RequirePermission permission="AUDIT_VIEW">
        <AuditLogPage />
      </RequirePermission>
    ),
  },
];
