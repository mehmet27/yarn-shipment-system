import { listRolePermissions } from '@iplik/api-client/role-permissions';
import { useQuery } from '@tanstack/react-query';

export const ROLE_PERMISSIONS_QUERY_KEY = ['role-permissions'] as const;

/** Rollerin geçerli izinleri, varsayılanları ve yalnızca yöneticide olan izinler. */
export function useRolePermissions() {
  return useQuery({ queryKey: ROLE_PERMISSIONS_QUERY_KEY, queryFn: listRolePermissions });
}
