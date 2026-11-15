import type { Permission, Role } from '@iplik/api-client';
import type { RolePermissionsView } from '@iplik/api-client/role-permissions';
import { http, HttpResponse } from 'msw';
import { PERMISSIONS_BY_ROLE } from './users';

const PATH = '/api/v1/role-permissions';
const ROLES: readonly Role[] = ['ADMIN', 'WAREHOUSE', 'QUALITY', 'MANAGER', 'GENERAL_MANAGER'];
const EDITABLE: readonly Role[] = ['WAREHOUSE', 'QUALITY', 'MANAGER', 'GENERAL_MANAGER'];
const LOCKED: readonly Permission[] = ['USER_MANAGE', 'SETTINGS_MANAGE', 'PERMISSION_MANAGE'];

let stored = initial();

function initial(): Record<Role, readonly Permission[]> {
  return { ...PERMISSIONS_BY_ROLE };
}

/** Testler arasında sahte rol izinlerini varsayılana döndürür. */
export function resetMockRolePermissions(): void {
  stored = initial();
}

function view(): RolePermissionsView {
  return {
    lockedPermissions: [...LOCKED],
    roles: ROLES.map((role) => ({
      role,
      editable: EDITABLE.includes(role),
      permissions: [...stored[role]],
      defaults: [...PERMISSIONS_BY_ROLE[role]],
    })),
  };
}

function problem(status: number, code: string) {
  return HttpResponse.json({ status, code, title: code }, { status });
}

function isRole(value: unknown): value is Role {
  return typeof value === 'string' && ROLES.some((role) => role === value);
}

function isPermissionList(value: unknown): value is { permissions: Permission[] } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'permissions' in value &&
    Array.isArray(value.permissions)
  );
}

/** Yetkiler ekranı uçları; yalnızca PERMISSION_MANAGE (yönetici), aksi halde 403. */
export function createRolePermissionHandlers(permissions: readonly Permission[]) {
  const canManage = permissions.includes('PERMISSION_MANAGE');
  return [
    http.get(PATH, () => (canManage ? HttpResponse.json(view()) : problem(403, 'FORBIDDEN'))),
    http.put(`${PATH}/:role`, async ({ params, request }) => {
      if (!canManage) return problem(403, 'FORBIDDEN');
      const body: unknown = await request.json();
      const role = params.role;
      if (!isRole(role) || !EDITABLE.includes(role) || !isPermissionList(body)) {
        return problem(400, 'INVALID_MASTER_DATA');
      }
      if (body.permissions.some((permission) => LOCKED.includes(permission))) {
        return problem(400, 'INVALID_MASTER_DATA');
      }
      stored = { ...stored, [role]: body.permissions };
      return HttpResponse.json(view());
    }),
    http.post(`${PATH}/:role/reset`, ({ params }) => {
      if (!canManage) return problem(403, 'FORBIDDEN');
      const role = params.role;
      if (!isRole(role) || !EDITABLE.includes(role)) return problem(400, 'INVALID_MASTER_DATA');
      stored = { ...stored, [role]: PERMISSIONS_BY_ROLE[role] };
      return HttpResponse.json(view());
    }),
  ];
}
