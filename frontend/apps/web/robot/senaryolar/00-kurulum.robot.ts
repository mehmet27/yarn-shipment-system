import { expect, rolTanimli, test } from '../support/robot';
import { sec } from '../support/yardimci';

/** Temiz kurulumda (yalnızca `admin` var) yönetici diğer kullanıcıları açar; her biri ilk girişte kendi şifresini seçer. */
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
      admin.kontrol(`${k.giris} zaten var`, true);
      continue;
    }
    await admin.adim(`${k.giris} hesabını aç (${k.rolAdi})`, async () => {
      await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
      const pencere = admin.sayfa.getByRole('dialog');
      await sec(admin.sayfa, /^Rol/, k.rolAdi, pencere);
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
      await sayfa.goto(
        `${process.env.ROBOT_GATEWAY ?? 'http://localhost:8080'}/oauth2/authorization/keycloak`,
      );
      await sayfa.fill('#username', k.giris);
      await sayfa.fill('#password', `${sifre}-ilk`);
      await sayfa.click('#kc-login');
      await sayfa.locator('#password-new').waitFor({ timeout: 20_000 });
      await sayfa.fill('#password-new', sifre);
      await sayfa.fill('#password-confirm', sifre);
      await sayfa.locator('input[type=submit], button[type=submit]').first().click();
      await sayfa.waitForURL(/5173/, { timeout: 20_000 });
      expect(sayfa.url()).toContain('/panel');
      await baglam.close();
    });
  }
});
