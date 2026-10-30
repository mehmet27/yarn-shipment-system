import type { Role } from './support/test';

export type Screen = {
  /** Rapor ve test adında görünen ad. */
  name: string;
  role: Role;
  path: string;
  /** Sayfa hazır olduğunda görünen başlık ya da metin. */
  ready: string | RegExp;
};

/**
 * Uygulamadaki her ekran. Yeni ekran eklerken buraya bir satır ekleyin: ekranlar.e2e.ts onu her
 * genişlikte açar, hatasız yüklendiğini, yatay taşma ve küçük dokunma hedefi olmadığını denetler.
 */
export const SCREENS: readonly Screen[] = [
  { name: 'Sevkiyat listesi', role: 'WAREHOUSE', path: '/panel/sevkiyat', ready: 'Sevkiyatlar' },
  {
    name: 'Yeni sevkiyat',
    role: 'WAREHOUSE',
    path: '/panel/sevkiyat/yeni',
    ready: 'Yeni sevkiyat',
  },
  {
    name: 'Sevkiyat dosyası',
    role: 'WAREHOUSE',
    path: '/panel/sevkiyat/shp-0001',
    ready: 'SVK-2026-0001',
  },
  {
    name: 'Çıkış tartısı',
    role: 'WAREHOUSE',
    path: '/panel/sevkiyat/shp-0001/cikis',
    ready: /Çıkış tartısı/,
  },
  {
    name: 'Sevkiyat dosyası (kalite)',
    role: 'QUALITY',
    path: '/panel/sevkiyat/shp-0007',
    ready: 'SVK-2026-0007',
  },
  { name: 'Tanımlar', role: 'ADMIN', path: '/panel/tanimlar', ready: 'Tanımlar' },
  {
    name: 'Çuval ve etiket',
    role: 'WAREHOUSE',
    path: '/panel/cuvallar',
    ready: 'Partinin çuvalları',
  },
  { name: 'Uyuşmazlıklar', role: 'ADMIN', path: '/panel/uyusmazliklar', ready: 'Uyuşmazlıklar' },
  {
    name: 'Uyuşmazlık detayı',
    role: 'ADMIN',
    path: '/panel/uyusmazliklar/disc-0',
    ready: 'UY-2026-0001',
  },
  { name: 'Saha listesi (şoför)', role: 'DRIVER', path: '/saha', ready: 'Bana atanan sevkiyatlar' },
  {
    name: 'Saha sevkiyatı (şoför)',
    role: 'DRIVER',
    path: '/saha/sevkiyat/shp-0010',
    ready: 'SVK-2026-0010',
  },
  {
    name: 'Saha listesi (fasoncu)',
    role: 'SUBCONTRACTOR',
    path: '/saha',
    ready: 'Bana atanan sevkiyatlar',
  },
  {
    name: 'Saha sevkiyatı (fasoncu)',
    role: 'SUBCONTRACTOR',
    path: '/saha/sevkiyat/shp-0004',
    ready: 'SVK-2026-0004',
  },
  {
    name: 'Teslim tutanağı',
    role: 'SUBCONTRACTOR',
    path: '/saha/sevkiyat/shp-0004/teslim',
    ready: /Teslim tutanağı/,
  },
];
