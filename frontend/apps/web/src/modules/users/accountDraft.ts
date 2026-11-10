import type { LoginAccountRequest, StaffLoginAccountRequest } from '@iplik/api-client';
import type { AccountKind } from './staffRoles';
import { isStaffKind, recordSlugOf } from './staffRoles';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const PHONE_SEPARATORS = /[\s().-]/g;
/** Türkiye cep numarası (0532…, 532…, 90532…, +90532…) ya da yurt dışı için + ile başlayan numara. */
const PHONE_PATTERN = /^(?:\+\d{8,15}|(?:\+?90|0)?5\d{9})$/;
const MIN_NAME_LENGTH = 2;

export type AccountDraft = {
  username: string;
  fullName: string;
  phone: string;
  email: string;
  kind: AccountKind | undefined;
  recordId: string | undefined;
  siteIds: readonly string[];
};

export type AccountField =
  'username' | 'fullName' | 'phone' | 'email' | 'role' | 'record' | 'sites';

export const EMPTY_DRAFT: AccountDraft = {
  username: '',
  fullName: '',
  phone: '',
  email: '',
  kind: undefined,
  recordId: undefined,
  siteIds: [],
};

export function isValidPhone(phone: string): boolean {
  return PHONE_PATTERN.test(phone.replace(PHONE_SEPARATORS, ''));
}

/** Kurala uymayan alanlar; boşsa form gönderilebilir. E-posta isteğe bağlıdır ama yazılmışsa geçerli olmalıdır. */
export function invalidFields(draft: AccountDraft): Set<AccountField> {
  const slug = recordSlugOf(draft.kind);
  const checks: readonly (readonly [AccountField, boolean])[] = [
    ['username', !USERNAME_PATTERN.test(draft.username)],
    ['role', draft.kind === undefined],
    ['record', slug !== undefined && draft.recordId === undefined],
    ['fullName', slug === undefined && draft.fullName.trim().length < MIN_NAME_LENGTH],
    ['phone', !isValidPhone(draft.phone)],
    ['email', draft.email !== '' && !EMAIL_PATTERN.test(draft.email)],
    ['sites', draft.siteIds.length === 0],
  ];
  return new Set(checks.filter(([, isInvalid]) => isInvalid).map(([field]) => field));
}

/** Hesabın hangi uca, hangi gövdeyle gideceği: fabrika içi roller tek uçtan, fasoncu kayıtlarından açılır. */
export type AccountSubmission =
  | { kind: 'staff'; request: StaffLoginAccountRequest }
  | { kind: 'subcontractor'; recordId: string; request: LoginAccountRequest };

/** Doğrulanmış taslaktan istek; rol ya da kayıt seçilmemişse gönderilecek bir şey yoktur. */
export function toSubmission(draft: AccountDraft): AccountSubmission | undefined {
  if (draft.kind === undefined) return undefined;
  const common = {
    username: draft.username,
    phone: draft.phone.trim(),
    siteIds: [...draft.siteIds],
    ...(draft.email === '' ? {} : { email: draft.email }),
  };
  const slug = recordSlugOf(draft.kind);
  if (slug !== undefined) {
    return draft.recordId === undefined
      ? undefined
      : { kind: slug, recordId: draft.recordId, request: common };
  }
  if (!isStaffKind(draft.kind)) return undefined;
  return {
    kind: 'staff',
    request: { ...common, fullName: draft.fullName.trim(), role: draft.kind },
  };
}
