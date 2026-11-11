import type { Permission } from '@iplik/api-client';
import type { ReactNode } from 'react';
import { Navigate } from 'react-router';
import { useCurrentUser } from '../hooks/useCurrentUser';

export type RequirePermissionProps = {
  /** Tek izin ya da izinlerden biri (herhangi biri yeterli). */
  permission: Permission | readonly Permission[];
  children: ReactNode;
};

/** Adresi elle yazan yetkisiz kullanıcı 403 sayfasına düşer. */
export function RequirePermission({ permission, children }: RequirePermissionProps) {
  const { data: user } = useCurrentUser();
  const required = typeof permission === 'string' ? [permission] : permission;
  const isAllowed = required.some((item) => user?.permissions.includes(item) ?? false);
  return isAllowed ? children : <Navigate to="/403" replace />;
}
