import type { LoginAccountView, StaffRole } from '@iplik/api-client';

/** Sonradan rolü değiştirilebilen fabrika içi roller (sunucudaki StaffRole). */
export const STAFF_ROLES: readonly StaffRole[] = ['ADMIN', 'WAREHOUSE', 'QUALITY', 'MANAGER'];

export function asStaffRole(role: string | null): StaffRole | undefined {
  return STAFF_ROLES.find((candidate) => candidate === role);
}

/** Rolü yalnızca tamamen fabrika içi hesaplarda değişir. */
export function hasChangeableRole(account: LoginAccountView): boolean {
  return account.roles.length > 0 && account.roles.every((role) => asStaffRole(role) !== undefined);
}
