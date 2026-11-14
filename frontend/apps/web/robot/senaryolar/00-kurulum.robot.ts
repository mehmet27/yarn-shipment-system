import { rolSifresi, rolTanimli, test } from '../support/robot';
import {
  adminSifreVer,
  girisDeneSonuc,
  girisFormu,
  sec,
  sifreDegistirFormu,
} from '../support/yardimci';

/**
 * Robotun koşusunu bağımsız kılar: yönetici eksik kullanıcıları açar; hesap zaten varsa ama robotun bildiği şifre
 * tutmuyorsa (ör. elle açılmış hesap) yönetici "Şifre ver" ile geçici şifre atar. Her iki durumda kullanıcı ilk girişte
 * robotun bildiği şifreyi seçer; böylece sonraki senaryolar elle belirlenmiş şifrelere takılmaz.
 */
const KULLANICILAR = [
  { rol: 'depo', giris: 'depo', ad: 'Davut Depocu', rolAdi: 'Depo' },
  { rol: 'kalite', giris: 'kalite', ad: 'Kerem Kalite', rolAdi: 'Kalite' },
  { rol: 'mudur', giris: 'mudur', ad: 'Melek Müdür', rolAdi: 'Müdür' },
  { rol: 'genel-mudur', giris: 'genel.mudur', ad: 'Genel Müdür', rolAdi: 'Genel müdür' },
] as const;

test('Kurulum: yönetici eksik kullanıcıları açar, her biri ilk girişi yapar', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/409 POST \/api\/v1\/master-data\/login-accounts/);
  await admin.sayfa.goto('/panel/kullanicilar');
  await admin.sayfa.waitForLoadState('networkidle');
  const liste = await admin.sayfa.innerText('main');

  for (const k of KULLANICILAR) {
    if (liste.includes(k.giris)) {
      const hedef = rolSifresi(k.rol) ?? sifre;
      await admin.adim(`${k.giris} zaten var: robotun şifresiyle girebiliyor mu`, async () => {
        const baglam = await browser.newContext({ locale: 'tr-TR' });
        const sayfa = await baglam.newPage();
        const girdi = await girisDeneSonuc(sayfa, k.giris, hedef);
        await baglam.close();
        if (girdi) return;
        // Şifre tutmuyor: yönetici geçici şifre verir, kullanıcı ilk girişte robotun şifresini seçer.
        await adminSifreVer(admin.sayfa, k.giris, `${hedef}-ilk`);
        const yeniBaglam = await browser.newContext({ locale: 'tr-TR' });
        const yeniSayfa = await yeniBaglam.newPage();
        await girisFormu(yeniSayfa, k.giris, `${hedef}-ilk`);
        await yeniSayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
        await sifreDegistirFormu(yeniSayfa, `${hedef}-ilk`, hedef);
        await yeniSayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
          timeout: 20_000,
        });
        await yeniBaglam.close();
      });
      continue;
    }
    await admin.adim(`${k.giris} hesabını aç (${k.rolAdi})`, async () => {
      await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
      const pencere = admin.sayfa.getByRole('dialog');
      await sec(admin.sayfa, /^Rol/, k.rolAdi, pencere);
      const alan = pencere.getByLabel(/Çalışma alanı/);
      // Kalite rolü bir çalışma alanı ister (merkez fabrika ya da fasoncular); ilk seçenek yeter.
      if ((await alan.count()) > 0) {
        await alan.first().click();
        await admin.sayfa.locator('[role=option]:visible').first().click();
      }
      await pencere.getByLabel(/Giriş adı/).fill(k.giris);
      await pencere.getByLabel(/Ad soyad/).fill(k.ad);
      await pencere.getByLabel(/İlk şifre/).fill(`${sifre}-ilk`);
      await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
      await admin.sayfa.getByText(`${k.giris} hesabı açıldı`).waitFor();
      await admin.sayfa.keyboard.press('Escape');
    });
    await admin.adim(`${k.giris} ilk girişi yapar ve kendi şifresini seçer`, async () => {
      const baglam = await browser.newContext({ locale: 'tr-TR' });
      const sayfa = await baglam.newPage();
      await girisFormu(sayfa, k.giris, `${sifre}-ilk`);
      await sayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
      await sifreDegistirFormu(sayfa, `${sifre}-ilk`, sifre);
      await sayfa.getByText(k.ad).first().waitFor({ timeout: 20_000 });
      await baglam.close();
    });
  }
});
