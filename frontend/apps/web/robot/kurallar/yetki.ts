/**
 * Yetki matrisi: hangi rol hangi ekranı açabilir. Robotun tek doğruluk kaynağı budur (01-gezinti ve 05-tum-ekranlar
 * buradan okur). Yeni ekran, rota ya da rol eklenince yalnızca bu dosya güncellenir.
 */
export const EKRANLAR = [
  '/panel',
  '/panel/sevkiyat',
  '/panel/sevkiyat/yeni',
  '/panel/uyusmazliklar',
  '/panel/tanimlar',
  '/panel/partiler',
  '/panel/raporlar',
  '/panel/bakiye',
  '/panel/faturalama',
  '/panel/merkez-stok',
  '/panel/kullanicilar',
  '/panel/yetkiler',
  '/panel/denetim',
  '/panel/gorunum',
] as const;

/** İzinli ekranlar; listede olmayan her ekran için 403 beklenir. */
export const IZINLI: Record<string, readonly string[]> = {
  // Yönetici yalnızca sistemi yönetir: iş ekranlarını (sevkiyat, uyuşmazlık, rapor, bakiye, stok) hiç görmez.
  // Yetkiler ekranı (PERMISSION_MANAGE) yalnızca yöneticidedir.
  admin: ['/panel', '/panel/kullanicilar', '/panel/yetkiler', '/panel/denetim', '/panel/gorunum'],
  depo: ['/panel', '/panel/sevkiyat', '/panel/merkez-stok'],
  kalite: [
    '/panel',
    '/panel/sevkiyat',
    '/panel/uyusmazliklar',
    '/panel/partiler',
    '/panel/raporlar',
    '/panel/bakiye',
    '/panel/merkez-stok',
    '/panel/denetim',
  ],
  mudur: [
    '/panel',
    '/panel/sevkiyat',
    '/panel/sevkiyat/yeni',
    '/panel/uyusmazliklar',
    '/panel/raporlar',
    '/panel/bakiye',
    '/panel/faturalama',
    '/panel/merkez-stok',
    '/panel/denetim',
    '/panel/tanimlar',
    '/panel/partiler',
  ],
  'genel-mudur': ['/panel', '/panel/raporlar', '/panel/bakiye', '/panel/merkez-stok'],
};
