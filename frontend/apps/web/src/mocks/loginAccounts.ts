import type { LoginAccountView, Role } from '@iplik/api-client';

/**
 * Keycloak'taki giriş hesaplarının sahte karşılığı; yerel realm'deki test kullanıcılarıyla aynı giriş adlarını taşır.
 * Şifre tutulmaz, gerçek uç da şifre döndürmez.
 */
const SEED: readonly LoginAccountView[] = [
  {
    id: 'kc-admin',
    username: 'admin',
    fullName: 'Sistem Yöneticisi',
    email: 'admin@iplik.local',
    roles: ['ADMIN'],
    enabled: true,
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

/** Tanımlar ekranından açılan hesap listeye ilk girişini bekleyen hesap olarak düşer. */
export function addMockLoginAccount(input: {
  username: string;
  email: string;
  fullName: string;
  role: Role;
}): void {
  accounts = [
    ...accounts,
    {
      id: `kc-new-${String(nextId++)}`,
      username: input.username,
      fullName: input.fullName,
      email: input.email,
      roles: [input.role],
      enabled: true,
      passwordChangeRequired: true,
      createdAt: new Date().toISOString(),
    },
  ];
}

export function resetMockLoginAccounts(): void {
  accounts = structuredClone([...SEED]);
  nextId = 1;
}
