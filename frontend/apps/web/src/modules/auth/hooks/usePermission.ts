import type { Permission } from '@iplik/api-client';
import { useCurrentUser } from './useCurrentUser';

/** FE-SEC-02: görünürlük yalnızca izin anahtarıyla; asıl kontrol backend'dedir. */
export function usePermission(permission: Permission): boolean {
  const { data: user } = useCurrentUser();
  return user?.permissions.includes(permission) ?? false;
}
