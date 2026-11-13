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
  '/panel/faturalanacaklar',
  '/panel/merkez-stok',
  '/panel/kullanicilar',
  '/panel/denetim',
  '/panel/gorunum',
] as const;

/** İzinli ekranlar; listede olmayan her ekran için 403 beklenir. */
export const IZINLI: Record<string, readonly string[]> = {
  admin: EKRANLAR.filter((e) => e !== '/panel/faturalanacaklar'),
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
    '/panel/faturalanacaklar',
    '/panel/merkez-stok',
    '/panel/denetim',
  ],
  'genel-mudur': ['/panel', '/panel/raporlar', '/panel/bakiye'],
};
