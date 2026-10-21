import type { Permission } from '@iplik/api-client';
import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { usePermission } from '../hooks/usePermission';

export type RequirePermissionProps = {
  permission: Permission;
  children: ReactNode;
};

/** Adresi elle yazan yetkisiz kullanıcı 403 sayfasına düşer. */
export function RequirePermission({ permission, children }: RequirePermissionProps) {
  const isAllowed = usePermission(permission);
  return isAllowed ? children : <Navigate to="/403" replace />;
}
