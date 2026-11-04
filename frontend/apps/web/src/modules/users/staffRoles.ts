import type { LoginAccountView, StaffRole } from '@iplik/api-client';

/** Sonradan rolü değiştirilebilen fabrika içi roller (sunucudaki StaffRole). */
export const STAFF_ROLES: readonly StaffRole[] = ['ADMIN', 'WAREHOUSE', 'QUALITY', 'MANAGER'];

/** Şoför ve fasoncu hesapları bir kayda bağlıdır; hesap açarken kayıt seçilir, rol sonradan değişmez. */
export type RecordKind = 'DRIVER' | 'SUBCONTRACTOR';
export const RECORD_KINDS: readonly RecordKind[] = ['DRIVER', 'SUBCONTRACTOR'];

/** Hesap açarken seçilebilen tüm girişli roller. */
export type AccountKind = StaffRole | RecordKind;
export const ACCOUNT_KINDS: readonly AccountKind[] = [...STAFF_ROLES, ...RECORD_KINDS];

export function asStaffRole(role: string | null): StaffRole | undefined {
  return STAFF_ROLES.find((candidate) => candidate === role);
}

export function asAccountKind(role: string | null): AccountKind | undefined {
  return ACCOUNT_KINDS.find((candidate) => candidate === role);
}

/** Kayda bağlı rolün kayıt türü (şoför ya da fasoncu); fabrika içi rollerde yoktur. */
export type RecordSlug = 'driver' | 'subcontractor';
const RECORD_SLUGS: Readonly<Record<RecordKind, RecordSlug>> = {
  DRIVER: 'driver',
  SUBCONTRACTOR: 'subcontractor',
};

export function recordSlugOf(kind: AccountKind | undefined): RecordSlug | undefined {
  return kind !== undefined && isRecordKind(kind) ? RECORD_SLUGS[kind] : undefined;
}

export function isStaffKind(kind: AccountKind): kind is StaffRole {
  return !isRecordKind(kind);
}

export function isRecordKind(kind: AccountKind): kind is RecordKind {
  return RECORD_KINDS.some((candidate) => candidate === kind);
}

/** Şoför ve fasoncu hesapları kayda bağlıdır; rolü yalnızca tamamen fabrika içi hesaplarda değişir. */
export function hasChangeableRole(account: LoginAccountView): boolean {
  return account.roles.length > 0 && account.roles.every((role) => asStaffRole(role) !== undefined);
}
