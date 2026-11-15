import type { Role } from '@iplik/api-client';
import type {
  RolePermissionsSaveRequest,
  RolePermissionsView,
} from '@iplik/api-client/role-permissions';
import { resetRolePermissions, saveRolePermissions } from '@iplik/api-client/role-permissions';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ROLE_PERMISSIONS_QUERY_KEY } from './useRolePermissions';

/** Yetkiler ekranının yazma çağrıları; dönen güncel liste önbelleğe yazılır. */
export function useRolePermissionMutations() {
  const queryClient = useQueryClient();
  const onSuccess = (view: RolePermissionsView) => {
    queryClient.setQueryData(ROLE_PERMISSIONS_QUERY_KEY, view);
  };
  const save = useMutation({
    mutationFn: ({ role, request }: { role: Role; request: RolePermissionsSaveRequest }) =>
      saveRolePermissions(role, request),
    onSuccess,
  });
  const reset = useMutation({
    mutationFn: (role: Role) => resetRolePermissions(role),
    onSuccess,
  });
  return { save, reset };
}
