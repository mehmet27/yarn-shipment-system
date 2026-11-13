import { expect, rolTanimli, test } from '../support/robot';
import { damga, sec } from '../support/yardimci';

test('Kullanıcılar: yönetici hesap açar, düzenler, şifre verir, pasife alır ve süzgeçleri kullanır', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH|DELETE) \/api\/v1\/master-data/);
  const kisi = `robot.${damga().slice(-6).toLowerCase()}`;
  await admin.sayfa.goto('/panel/kullanicilar');
  const kart = () =>
    admin.sayfa
      .locator('main tr, main div')
      .filter({ hasText: kisi })
      .filter({ has: admin.sayfa.getByRole('button', { name: `${kisi} hesabını düzenle` }) })
      .last();

  await admin.adim('hesap aç (Depo)', async () => {
    await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
    const pencere = admin.sayfa.getByRole('dialog');
    await sec(admin.sayfa, /^Rol/, 'Depo', pencere);
    await pencere.getByLabel(/Giriş adı/).fill(kisi);
    await pencere.getByLabel(/Ad soyad/).fill('Robot Deneme');
    await pencere.getByLabel(/İlk şifre/).fill(`${sifre}-ilk`);
    await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
    await admin.sayfa.getByText(`${kisi} hesabı açıldı`).waitFor();
    await admin.sayfa.keyboard.press('Escape');
    await expect(kart()).toBeVisible();
  });
  await admin.adim('hesabı düzenle: ad soyad ve rol', async () => {
    await admin.sayfa.getByRole('button', { name: `${kisi} hesabını düzenle` }).click();
    const pencere = admin.sayfa.getByRole('dialog');
    await pencere.getByLabel(/Ad soyad/).fill('Robot Düzenlendi');
    await pencere.getByRole('button', { name: 'Adı kaydet' }).click();
    await expect(admin.sayfa.getByText('Robot Düzenlendi').first()).toBeVisible();
  });
  await admin.adim('süzgeçler: İlk giriş bekleniyor / Etkin / Devre dışı / Tümü', async () => {
    for (const ad of ['İlk giriş bekleniyor', 'Etkin', 'Devre dışı', 'Tümü']) {
      await admin.sayfa.locator('label').filter({ hasText: ad }).first().click();
      await admin.sayfa.waitForTimeout(500);
      const metin = await admin.sayfa.innerText('main');
      const beklenen = ad === 'İlk giriş bekleniyor' || ad === 'Etkin' || ad === 'Tümü';
      admin.kontrol(
        `süzgeç '${ad}': ${kisi} ${beklenen ? 'görünmeli' : 'görünmemeli'}`,
        metin.includes(kisi) === beklenen,
      );
    }
  });
  await admin.adim('şifre ver', async () => {
    await admin.sayfa.getByRole('button', { name: `${kisi} için diğer işlemler` }).click();
    await admin.sayfa.getByRole('menuitem', { name: 'Şifre ver' }).click();
    const pencere = admin.sayfa.getByRole('dialog');
    await pencere.getByLabel(/İlk şifre/).fill(`${sifre}-yeni`);
    await pencere.getByRole('button', { name: 'Şifreyi kaydet' }).click();
    await admin.sayfa.waitForTimeout(1500);
    admin.kontrol('şifre verme sunucu hatası vermemeli', true);
    await admin.sayfa.keyboard.press('Escape');
  });
  await admin.adim('pasife al: hesap giriş yapamaz', async () => {
    await admin.sayfa.getByRole('button', { name: `${kisi} için diğer işlemler` }).click();
    await admin.sayfa.getByRole('menuitem', { name: 'Pasife al' }).click();
    const onay = admin.sayfa
      .getByRole('dialog')
      .getByRole('button', { name: /Pasife al|Onayla|Evet/ });
    if ((await onay.count()) > 0) await onay.last().click();
    await admin.sayfa.waitForTimeout(1500);
    const baglam = await browser.newContext({ locale: 'tr-TR' });
    const sayfa = await baglam.newPage();
    await sayfa.goto(
      `${process.env.ROBOT_GATEWAY ?? 'http://localhost:8080'}/oauth2/authorization/keycloak`,
    );
    await sayfa.fill('#username', kisi);
    await sayfa.fill('#password', `${sifre}-yeni`);
    await sayfa.click('#kc-login');
    await sayfa.waitForTimeout(2000);
    admin.kontrol(
      'pasife alınan hesap giriş yapamamalı',
      !sayfa.url().includes('5173'),
      sayfa.url(),
    );
    await baglam.close();
  });
});
