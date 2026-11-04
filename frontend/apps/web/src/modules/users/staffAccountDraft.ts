import type { StaffLoginAccountRequest, StaffRole } from '@iplik/api-client';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const MIN_NAME_LENGTH = 2;

export type StaffAccountDraft = {
  username: string;
  fullName: string;
  email: string;
  role: StaffRole | undefined;
  siteIds: readonly string[];
};

export type StaffAccountField = 'username' | 'fullName' | 'email' | 'role' | 'sites';

export const EMPTY_DRAFT: StaffAccountDraft = {
  username: '',
  fullName: '',
  email: '',
  role: undefined,
  siteIds: [],
};

/** Kurala uymayan alanlar; boşsa form gönderilebilir. */
export function invalidFields(draft: StaffAccountDraft): Set<StaffAccountField> {
  const invalid = new Set<StaffAccountField>();
  if (!USERNAME_PATTERN.test(draft.username)) invalid.add('username');
  if (draft.fullName.trim().length < MIN_NAME_LENGTH) invalid.add('fullName');
  if (!EMAIL_PATTERN.test(draft.email)) invalid.add('email');
  if (draft.role === undefined) invalid.add('role');
  if (draft.siteIds.length === 0) invalid.add('sites');
  return invalid;
}

/** Doğrulanmış taslaktan istek; rol seçilmemişse gönderilecek bir şey yoktur. */
export function toRequest(draft: StaffAccountDraft): StaffLoginAccountRequest | undefined {
  if (draft.role === undefined) return undefined;
  return {
    username: draft.username,
    email: draft.email,
    fullName: draft.fullName.trim(),
    role: draft.role,
    siteIds: [...draft.siteIds],
  };
}
