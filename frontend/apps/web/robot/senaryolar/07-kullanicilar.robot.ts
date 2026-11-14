import { expect, rolTanimli, test } from '../support/robot';
import { adminSifreVer, damga, girisFormu, sec, sifreDegistirFormu } from '../support/yardimci';

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
  await admin.adim(
    'şifre ver: eski şifre geçmez, yeni geçici şifre zorunlu değişim ister',
    async () => {
      await adminSifreVer(admin.sayfa, kisi, `${sifre}-yeni`);
      const baglam = await browser.newContext({ locale: 'tr-TR' });
      const sayfa = await baglam.newPage();
      await girisFormu(sayfa, kisi, `${sifre}-ilk`);
      await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
      await girisFormu(sayfa, kisi, `${sifre}-yeni`);
      await sayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
      await sifreDegistirFormu(sayfa, `${sifre}-yeni`, `${sifre}-kendi`);
      await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
        timeout: 20_000,
      });
      await baglam.close();
    },
  );
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
    await girisFormu(sayfa, kisi, `${sifre}-kendi`);
    await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
    admin.kontrol('pasife alınan hesap giriş yapamamalı', /giris/.test(sayfa.url()), sayfa.url());
    await baglam.close();
  });
});

test('Giriş: hatalı şifre, hesap kilidi ve geçici şifreyle zorunlu şifre değişikliği', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH|DELETE) \/api\/v1\/master-data/);
  const kisi = `giris.${damga().slice(-6).toLowerCase()}`;
  await admin.sayfa.goto('/panel/kullanicilar');

  await admin.adim('geçici şifreli hesap aç (Depo)', async () => {
    await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
    const pencere = admin.sayfa.getByRole('dialog');
    await sec(admin.sayfa, /^Rol/, 'Depo', pencere);
    await pencere.getByLabel(/Giriş adı/).fill(kisi);
    await pencere.getByLabel(/Ad soyad/).fill('Robot Giriş');
    await pencere.getByLabel(/İlk şifre/).fill(`${sifre}-ilk`);
    await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
    await admin.sayfa.getByText(`${kisi} hesabı açıldı`).waitFor();
    await admin.sayfa.keyboard.press('Escape');
  });

  const baglam = await browser.newContext({ locale: 'tr-TR' });
  const sayfa = await baglam.newPage();
  await admin.adim('korumasız sayfa giriş sayfasına yönlendirir', async () => {
    await sayfa.goto('/panel/sevkiyat');
    await sayfa.waitForURL(/giris\?returnTo/);
  });
  await admin.adim('hatalı şifre: kullanıcı adı mı şifre mi olduğu söylenmez', async () => {
    await girisFormu(sayfa, kisi, 'yanlis-sifre-1');
    await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
    await girisFormu(sayfa, 'olmayan.kisi', 'yanlis-sifre-1');
    await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
  });
  await admin.adim(
    'geçici şifreyle giren başka sayfaya geçemez, şifresini değiştirir',
    async () => {
      await girisFormu(sayfa, kisi, `${sifre}-ilk`);
      await sayfa.waitForURL(/sifre-degistir/);
      await sayfa.goto('/panel');
      await sayfa.waitForURL(/sifre-degistir/);
      await sifreDegistirFormu(sayfa, `${sifre}-ilk`, `${sifre}-kendi`);
      await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname));
    },
  );
  await admin.adim('çıkış yapınca oturum kapanır', async () => {
    await sayfa.getByRole('button', { name: 'Robot Giriş' }).click();
    await sayfa.getByRole('menuitem', { name: 'Çıkış yap' }).click();
    await sayfa.waitForURL(/giris/);
    await sayfa.goto('/panel');
    await sayfa.waitForURL(/giris\?returnTo/);
  });
  await admin.adim('5 hatalı denemeden sonra hesap kilitlenir', async () => {
    for (let i = 0; i < 5; i += 1) {
      await girisFormu(sayfa, kisi, `yanlis-sifre-${String(i)}`);
      await sayfa
        .getByText(/hatalı|kilitlendi/)
        .first()
        .waitFor();
    }
    // Kilitliyken doğru şifre de kabul edilmez.
    await girisFormu(sayfa, kisi, `${sifre}-kendi`);
    await sayfa.getByText(/hesap kısa süre kilitlendi/).waitFor();
  });
  await baglam.close();
});

test('Şifre değiştirme: kullanıcı kendi şifresini menüden değiştirir', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH|DELETE) \/api\/v1\/master-data/);
  const kisi = `sifre.${damga().slice(-6).toLowerCase()}`;
  const ad = 'Robot Şifre';
  const ilk = `${sifre}-ilk`;
  const kendi = `${sifre}-kendi`;
  const yeni = `${sifre}-yeni2`;
  await admin.sayfa.goto('/panel/kullanicilar');
  await admin.adim('hesap aç (Depo)', async () => {
    await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
    const pencere = admin.sayfa.getByRole('dialog');
    await sec(admin.sayfa, /^Rol/, 'Depo', pencere);
    await pencere.getByLabel(/Giriş adı/).fill(kisi);
    await pencere.getByLabel(/Ad soyad/).fill(ad);
    await pencere.getByLabel(/İlk şifre/).fill(ilk);
    await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
    await admin.sayfa.getByText(`${kisi} hesabı açıldı`).waitFor();
    await admin.sayfa.keyboard.press('Escape');
  });

  const baglam = await browser.newContext({ locale: 'tr-TR' });
  const sayfa = await baglam.newPage();
  await admin.adim(
    'ilk giriş: zorunlu değişimde "Vazgeç" yok, uyumsuz şifre engellenir',
    async () => {
      await girisFormu(sayfa, kisi, ilk);
      await sayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
      admin.kontrol(
        'zorunlu değişimde Vazgeç görünmemeli',
        (await sayfa.getByRole('link', { name: 'Vazgeç' }).count()) === 0,
      );
      await sayfa.getByLabel(/^Mevcut şifre/).fill(ilk);
      await sayfa.getByLabel(/^Yeni şifre( \*)?$/).fill(kendi);
      await sayfa.getByLabel(/^Yeni şifre \(tekrar\)/).fill(`${kendi}x`);
      await sayfa.getByText('Yeni şifreler aynı değil.').waitFor();
      admin.kontrol(
        'şifreler uyuşmazken gönder düğmesi kapalı olmalı',
        await sayfa.getByRole('button', { name: 'Şifreyi değiştir' }).isDisabled(),
      );
      await sayfa.getByLabel(/^Yeni şifre \(tekrar\)/).fill(kendi);
      await sayfa.getByRole('button', { name: 'Şifreyi değiştir' }).click();
      await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
        timeout: 20_000,
      });
    },
  );
  await admin.adim('menüden "Şifremi değiştir": Vazgeç görünür ve geri döner', async () => {
    await sayfa.getByRole('button', { name: ad }).click();
    await sayfa.getByRole('menuitem', { name: 'Şifremi değiştir' }).click();
    await sayfa.waitForURL(/sifre-degistir/);
    await sayfa.getByRole('link', { name: 'Vazgeç' }).click();
    await sayfa.waitForURL((adres) => !/sifre-degistir/.test(adres.pathname));
  });
  await admin.adim('hatalı mevcut şifre ve zayıf yeni şifre nedeniyle reddedilir', async () => {
    await sayfa.getByRole('button', { name: ad }).click();
    await sayfa.getByRole('menuitem', { name: 'Şifremi değiştir' }).click();
    await sayfa.waitForURL(/sifre-degistir/);
    await sifreDegistirFormu(sayfa, 'yanlis-mevcut-sifre', yeni);
    await sayfa.getByText('Mevcut şifre hatalı.').waitFor();
    await sifreDegistirFormu(sayfa, kendi, '12345678');
    await sayfa.getByText(/Yeni şifre kurallara uymuyor/).waitFor();
    admin.kontrol('reddedilince sayfada kalmalı', /sifre-degistir/.test(sayfa.url()), sayfa.url());
  });
  await admin.adim('doğru bilgilerle şifre değişir; eski şifre geçmez, yenisi geçer', async () => {
    await sifreDegistirFormu(sayfa, kendi, yeni);
    await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
      timeout: 20_000,
    });
    await sayfa.getByRole('button', { name: ad }).click();
    await sayfa.getByRole('menuitem', { name: 'Çıkış yap' }).click();
    await sayfa.waitForURL(/giris/);
    await girisFormu(sayfa, kisi, kendi);
    await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
    await girisFormu(sayfa, kisi, yeni);
    await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
      timeout: 20_000,
    });
    admin.kontrol('yeni şifreyle zorunlu değişim istenmemeli', !/sifre-degistir/.test(sayfa.url()));
  });
  await baglam.close();
});
