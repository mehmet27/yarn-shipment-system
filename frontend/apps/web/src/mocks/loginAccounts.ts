import type { LoginAccountView, Role, WorkArea } from '@iplik/api-client';

/**
 * Keycloak'taki giriş hesaplarının sahte karşılığı; yerel realm'deki test kullanıcılarıyla aynı giriş adlarını taşır.
 * Şifre tutulmaz, gerçek uç da şifre döndürmez.
 */
const SEED: readonly LoginAccountView[] = [
  {
    id: 'kc-admin',
    username: 'admin',
    fullName: 'Sistem Yöneticisi',
    roles: ['ADMIN'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:00:00Z',
  },
  {
    id: 'kc-depo',
    username: 'depo',
    fullName: 'Depo Sorumlusu',
    roles: ['WAREHOUSE'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:05:00Z',
  },
  {
    id: 'kc-kalite',
    username: 'kalite',
    fullName: 'Kalite Uzmanı',
    roles: ['QUALITY'],
    workArea: 'CENTRAL',
    enabled: true,
    passwordChangeRequired: true,
    createdAt: '2026-01-05T08:10:00Z',
  },
  {
    id: 'kc-mudur',
    username: 'mudur',
    fullName: 'Fabrika Müdürü',
    roles: ['MANAGER'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:15:00Z',
  },
  {
    id: 'kc-genelmudur',
    username: 'genelmudur',
    fullName: 'Genel Müdür',
    roles: ['GENERAL_MANAGER'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:20:00Z',
  },
  {
    id: 'kc-eski',
    username: 'eski.depo',
    roles: ['WAREHOUSE'],
    enabled: false,
    passwordChangeRequired: false,
    createdAt: '2025-11-20T07:00:00Z',
  },
];

let accounts: LoginAccountView[] = structuredClone([...SEED]);
let nextId = 1;

/** Gerçek uç gibi giriş adına göre sıralı. */
export function listMockLoginAccounts(): readonly LoginAccountView[] {
  return [...accounts].sort((a, b) => a.username.localeCompare(b.username));
}

export function isMockUsernameTaken(username: string): boolean {
  return accounts.some((account) => account.username === username);
}

/** Açılan hesap, kişi geçici şifresini değiştirene kadar ilk girişini bekleyen hesap olarak listeye düşer. */
export function addMockLoginAccount(input: {
  username: string;
  fullName: string;
  role: Role;
  workArea?: WorkArea | undefined;
}): void {
  accounts = [
    ...accounts,
    {
      id: `kc-new-${String(nextId++)}`,
      username: input.username,
      fullName: input.fullName,
      roles: [input.role],
      ...(input.workArea === undefined ? {} : { workArea: input.workArea }),
      enabled: true,
      passwordChangeRequired: true,
      createdAt: new Date().toISOString(),
    },
  ];
}

/** Hesabı açar ya da kapatır; bulunamazsa `false` döner. */
export function setMockLoginAccountEnabled(id: string, enabled: boolean): boolean {
  if (!accounts.some((candidate) => candidate.id === id)) return false;
  accounts = accounts.map((candidate) =>
    candidate.id === id ? { ...candidate, enabled } : candidate,
  );
  return true;
}

/** Yeni geçici şifre verilince hesap ilk giriş bekleyen olur; bulunamazsa `false` döner. */
export function setMockLoginAccountPassword(id: string): boolean {
  if (!accounts.some((candidate) => candidate.id === id)) return false;
  accounts = accounts.map((candidate) =>
    candidate.id === id ? { ...candidate, passwordChangeRequired: true } : candidate,
  );
  return true;
}

/** Ad soyadı değiştirir; bulunamazsa `false` döner. */
export function changeMockLoginAccountName(id: string, fullName: string): boolean {
  if (!accounts.some((candidate) => candidate.id === id)) return false;
  accounts = accounts.map((candidate) =>
    candidate.id === id ? { ...candidate, fullName } : candidate,
  );
  return true;
}

/** Rolü değiştirir; bulunamazsa `not-found` döner. */
export function changeMockLoginAccountRole(
  id: string,
  role: Role,
  workArea?: WorkArea,
): 'ok' | 'not-found' {
  const account = accounts.find((candidate) => candidate.id === id);
  if (account === undefined) return 'not-found';
  accounts = accounts.map((candidate) => {
    if (candidate.id !== id) return candidate;
    const changed: LoginAccountView = { ...candidate, roles: [role] };
    if (workArea === undefined) delete changed.workArea;
    else changed.workArea = workArea;
    return changed;
  });
  return 'ok';
}

export function resetMockLoginAccounts(): void {
  accounts = structuredClone([...SEED]);
  nextId = 1;
}
