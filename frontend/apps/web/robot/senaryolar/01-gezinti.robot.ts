import { rolTanimli, test } from '../support/robot';

/** Her rol için hangi ekranların açılması, hangilerinin 403 vermesi gerektiği (yetkilendirme matrisi). */
const EKRANLAR = [
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

/** İzinli ekranlar; listede olmayan her ekran için 403 beklenir. Yeni ekran/rol eklenince burası güncellenir. */
const IZINLI: Record<string, readonly string[]> = {
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
  'fason-kalite': [
    '/panel',
    '/panel/sevkiyat',
    '/panel/uyusmazliklar',
    '/panel/partiler',
    '/panel/raporlar',
    '/panel/bakiye',
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

for (const [rol, izinli] of Object.entries(IZINLI)) {
  test(`Gezinti: ${rol} tüm ekranları açar ve yetkisizlerde 403 görür`, async ({ giris }) => {
    test.skip(!rolTanimli(rol), `${rol} için kullanıcı/şifre tanımlı değil`);
    const robot = await giris(rol);
    for (const yol of EKRANLAR) {
      await robot.adim(`${yol} aç`, async () => {
        const yetkili = izinli.includes(yol);
        if (!yetkili) robot.beklenen(/403 GET/);
        await robot.sayfa.goto(yol);
        await robot.sayfa.waitForLoadState('networkidle');
        await robot.sayfa.waitForTimeout(500);
        const bulunan = new URL(robot.sayfa.url()).pathname;
        const metin = await robot.sayfa.innerText('body');
        robot.kontrol(
          `${yol}: ${yetkili ? 'açılmalı' : '403 vermeli'}`,
          yetkili
            ? bulunan === yol &&
                !/Beklenmeyen bir hata|Aradığınız sayfa bulunamadı|Bu sayfayı görme yetkiniz yok/.test(
                  metin,
                )
            : bulunan === '/403',
          `adres: ${bulunan}`,
        );
      });
    }
  });
}
