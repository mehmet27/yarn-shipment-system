import type { StaffLoginAccountRequest } from '@iplik/api-client';
import type { StaffRole, WorkArea } from '@iplik/api-client';
import { needsWorkArea } from './staffRoles';

const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const MIN_NAME_LENGTH = 2;
/** Parola politikası (Keycloak ve servis): en az 8 karakter. */
export const MIN_PASSWORD_LENGTH = 8;

export type AccountDraft = {
  username: string;
  fullName: string;
  password: string;
  kind: StaffRole | undefined;
  /** Yalnız kalite rolünde sorulur ve zorunludur. */
  workArea: WorkArea | undefined;
};

export type AccountField = 'username' | 'fullName' | 'password' | 'role' | 'workArea';

export const EMPTY_DRAFT: AccountDraft = {
  username: '',
  fullName: '',
  password: '',
  kind: undefined,
  workArea: undefined,
};

/** Kurala uymayan alanlar; boşsa form gönderilebilir. */
export function invalidFields(draft: AccountDraft): Set<AccountField> {
  const checks: readonly (readonly [AccountField, boolean])[] = [
    ['username', !USERNAME_PATTERN.test(draft.username)],
    ['role', draft.kind === undefined],
    ['workArea', needsWorkArea(draft.kind) && draft.workArea === undefined],
    ['fullName', draft.fullName.trim().length < MIN_NAME_LENGTH],
    ['password', draft.password.length < MIN_PASSWORD_LENGTH],
  ];
  return new Set(checks.filter(([, isInvalid]) => isInvalid).map(([field]) => field));
}

/** Hesap yalnızca fabrika personeli içindir (KR-17); gövde tek uca gider. */
export type AccountSubmission = { kind: 'staff'; request: StaffLoginAccountRequest };

/** Doğrulanmış taslaktan istek; rol seçilmemişse gönderilecek bir şey yoktur. */
export function toSubmission(draft: AccountDraft): AccountSubmission | undefined {
  if (draft.kind === undefined) return undefined;
  return {
    kind: 'staff',
    request: {
      username: draft.username,
      fullName: draft.fullName.trim(),
      role: draft.kind,
      password: draft.password,
      ...(needsWorkArea(draft.kind) && draft.workArea !== undefined
        ? { workArea: draft.workArea }
        : {}),
    },
  };
}
