import type { LoginAccountView } from '@iplik/api-client';

export type StatusFilter = 'ALL' | 'ENABLED' | 'DISABLED' | 'PENDING';
export type AccountFilter = { status: StatusFilter; role: string | null };

export const NO_FILTER: AccountFilter = { status: 'ALL', role: null };

export function isFiltering(filter: AccountFilter): boolean {
  return filter.status !== 'ALL' || filter.role !== null;
}

/** Sunucu liste süzgeci sunmadığı için yalnız yüklü sayfadaki satırları süzer. */
export function applyFilter(
  accounts: readonly LoginAccountView[],
  filter: AccountFilter,
): LoginAccountView[] {
  return accounts.filter((account) => {
    if (filter.role !== null && !account.roles.some((role) => role === filter.role)) return false;
    switch (filter.status) {
      case 'ENABLED':
        return account.enabled;
      case 'DISABLED':
        return !account.enabled;
      case 'PENDING':
        return account.passwordChangeRequired;
      case 'ALL':
        return true;
    }
  });
}
