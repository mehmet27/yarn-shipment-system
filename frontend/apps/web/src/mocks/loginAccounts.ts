import type { LoginAccountView, Role } from '@iplik/api-client';

/**
 * Keycloak'taki giriş hesaplarının sahte karşılığı; yerel realm'deki test kullanıcılarıyla aynı giriş adlarını taşır.
 * Şifre tutulmaz, gerçek uç da şifre döndürmez; telefon yalnızca maskeli gelir.
 */
const SEED: readonly LoginAccountView[] = [
  {
    id: 'kc-admin',
    username: 'admin',
    fullName: 'Sistem Yöneticisi',
    email: 'admin@iplik.local',
    roles: ['ADMIN'],
    enabled: true,
    phone: '***0001',
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:00:00Z',
  },
  {
    id: 'kc-depo',
    username: 'depo',
    fullName: 'Depo Sorumlusu',
    email: 'depo@iplik.local',
    roles: ['WAREHOUSE'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:05:00Z',
  },
  {
    id: 'kc-kalite',
    username: 'kalite',
    fullName: 'Kalite Uzmanı',
    email: 'kalite@iplik.local',
    roles: ['QUALITY'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:10:00Z',
  },
  {
    id: 'kc-mudur',
    username: 'mudur',
    fullName: 'Fabrika Müdürü',
    email: 'mudur@iplik.local',
    roles: ['MANAGER'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-01-05T08:15:00Z',
  },
  {
    id: 'kc-sofor',
    username: 'sofor',
    fullName: 'Şahin Şoför',
    email: 'sofor@iplik.local',
    roles: ['DRIVER'],
    enabled: true,
    phone: '***0005',
    passwordChangeRequired: true,
    createdAt: '2026-02-10T09:30:00Z',
  },
  {
    id: 'kc-fasoncu',
    username: 'fasoncu',
    fullName: 'Aydın Örme',
    email: 'fasoncu@aydinorme.local',
    roles: ['SUBCONTRACTOR'],
    enabled: true,
    passwordChangeRequired: false,
    createdAt: '2026-02-10T09:45:00Z',
  },
  {
    id: 'kc-eski',
    username: 'eski.depo',
    email: 'eski.depo@iplik.local',
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

/** Açılan hesap, kişi SMS'le şifresini belirleyene kadar ilk girişini bekleyen hesap olarak listeye düşer. */
export function addMockLoginAccount(input: {
  username: string;
  email?: string;
  phone: string;
  fullName: string;
  role: Role;
}): void {
  accounts = [
    ...accounts,
    {
      id: `kc-new-${String(nextId++)}`,
      username: input.username,
      fullName: input.fullName,
      ...(input.email === undefined || input.email === '' ? {} : { email: input.email }),
      phone: `***${input.phone.replace(/\D/g, '').slice(-4)}`,
      roles: [input.role],
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

/** Yönetici sıfırlaması için hesabın telefonu var mı; yoksa `no-phone`, hesap yoksa `not-found`. */
export function mockPasswordResetState(id: string): 'ok' | 'not-found' | 'no-phone' {
  const account = accounts.find((candidate) => candidate.id === id);
  if (account === undefined) return 'not-found';
  return account.phone === undefined ? 'no-phone' : 'ok';
}

/** Rolü değiştirir; bulunamazsa `not-found`, şoför/fasoncu hesabıysa `locked` döner. */
export function changeMockLoginAccountRole(id: string, role: Role): 'ok' | 'not-found' | 'locked' {
  const account = accounts.find((candidate) => candidate.id === id);
  if (account === undefined) return 'not-found';
  if (account.roles.includes('DRIVER') || account.roles.includes('SUBCONTRACTOR')) return 'locked';
  accounts = accounts.map((candidate) =>
    candidate.id === id ? { ...candidate, roles: [role] } : candidate,
  );
  return 'ok';
}

export function resetMockLoginAccounts(): void {
  accounts = structuredClone([...SEED]);
  nextId = 1;
}
