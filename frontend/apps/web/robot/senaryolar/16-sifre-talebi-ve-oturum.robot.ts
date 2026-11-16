import type { Page } from '@playwright/test';
import { expect, rolTanimli, test } from '../support/robot';
import {
  adminSifreVer,
  damga,
  depoHesabiAc,
  girisFormu,
  ilkGirisVeSifreSec,
  oturumDurumu,
  sifreDegistirFormu,
} from '../support/yardimci';

const TALEP_DUGMESI = 'Adminden şifre talep et';

/**
 * Hatalı şifreyle bir giriş denemesi yapar ve sunucunun yanıtını bekler. Yanıt dinleyicisi tıklamadan ÖNCE kurulur:
 * sunucu hızlı yanıt verince dinleyici sonradan kurulursa yanıt kaçar ve bekleme zaman aşımına düşer.
 * İlk deneme formu açar; sonrakiler aynı sayfada şifreyi yeniden yazar (hatalı deneme sayacı sunucuda tutulur).
 */
async function hataliGiris(sayfa: Page, kisi: string, deneme: number) {
  const yanit = sayfa.waitForResponse((y) => y.url().includes('/api/v1/auth/login'));
  if (deneme === 1) {
    await girisFormu(sayfa, kisi, `yanlis-${String(deneme)}`);
  } else {
    await sayfa.getByLabel(/^Şifre/).fill(`yanlis-${String(deneme)}`);
    await sayfa.getByRole('button', { name: 'Giriş yap' }).click();
  }
  await yanit;
}

test('Şifre talebi: 3 hatalı girişte adminden şifre istenir; admin görür, şifre verir, talep kapanır', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH|DELETE) \/api\/v1\/(master-data|auth)/);
  const kisi = `talep.${damga().slice(-6).toLowerCase()}`;
  const ad = 'Robot Talep';
  const ilk = `${sifre}-ilk`;
  const kendi = `${sifre}-kendi`;
  const gecici = `${sifre}-gecici`;
  const yeni = `${sifre}-yeni3`;
  await admin.adim('hesap açılır ve kişi kendi şifresini seçer', async () => {
    await depoHesabiAc(admin.sayfa, kisi, ad, ilk);
    const baglam = await browser.newContext({ locale: 'tr-TR' });
    await ilkGirisVeSifreSec(await baglam.newPage(), kisi, ilk, kendi);
    await baglam.close();
  });

  const baglam = await browser.newContext({ locale: 'tr-TR' });
  const sayfa = await baglam.newPage();
  await admin.adim(
    'giriş: ilk iki hatalı denemede öneri yok, üçüncüde "Adminden şifre talep et" çıkar',
    async () => {
      for (let deneme = 1; deneme <= 3; deneme += 1) {
        // Sayaç sayfa bellekte tutulur: ilk deneme formu açar, sonrakiler aynı sayfada şifreyi yeniden yazar.
        await hataliGiris(sayfa, kisi, deneme);
        await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
        const adet = await sayfa.getByRole('button', { name: TALEP_DUGMESI }).count();
        admin.kontrol(
          `${String(deneme)}. hatalı denemeden sonra talep düğmesi ${deneme < 3 ? 'olmamalı' : 'olmalı'}`,
          deneme < 3 ? adet === 0 : adet === 1,
        );
      }
      await sayfa.getByRole('button', { name: TALEP_DUGMESI }).click();
      await expect(sayfa.getByText(/Talebiniz yöneticiye iletildi/)).toBeVisible();
    },
  );

  await admin.adim('hatasız hesabın talebi kaydedilmez (sunucu koşulu denetler)', async () => {
    const kod = await sayfa.evaluate(async () => {
      const yanit = await fetch('/api/v1/auth/password-requests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'kalite' }),
      });
      return yanit.status;
    });
    admin.kontrol('talep cevabı hesabın durumunu sızdırmadan 204 olmalı', kod === 204, String(kod));
  });

  await admin.adim('admin hesabı için talep düğmesi çıkmaz, açıklama görünür', async () => {
    const adminSayfa = await (await browser.newContext({ locale: 'tr-TR' })).newPage();
    for (let deneme = 1; deneme <= 3; deneme += 1) {
      await hataliGiris(adminSayfa, 'admin', deneme);
      await adminSayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
    }
    await expect(adminSayfa.getByText(/Yönetici hesabı için şifre talebi/)).toBeVisible();
    admin.kontrol(
      'admin için talep düğmesi olmamalı',
      (await adminSayfa.getByRole('button', { name: TALEP_DUGMESI }).count()) === 0,
    );
  });

  await admin.adim('admin: Kullanıcılar ekranında talebi görür', async () => {
    await admin.sayfa.goto('/panel/kullanicilar');
    await expect(admin.sayfa.getByText(/1 kullanıcı şifre istedi/)).toBeVisible();
    const satir = admin.sayfa.getByRole('row', { name: new RegExp(kisi) });
    await expect(satir.getByText('Şifre istedi')).toBeVisible();
    const kalite = admin.sayfa.getByRole('row', { name: /kalite/ }).first();
    admin.kontrol(
      'talep etmeyen hesapta rozet olmamalı',
      (await kalite.getByText('Şifre istedi').count()) === 0,
    );
    await admin.sayfa.getByRole('button', { name: 'Göster' }).click();
    await expect(admin.sayfa.getByRole('row', { name: new RegExp(kisi) })).toBeVisible();
    admin.kontrol(
      'Göster süzgeci yalnızca şifre isteyen hesabı bırakmalı',
      (await admin.sayfa.getByRole('row', { name: /kalite/ }).count()) === 0,
    );
  });

  await admin.adim(
    'admin geçici şifre verir; talep kapanır, kişi yeni şifre seçerek girer',
    async () => {
      await adminSifreVer(admin.sayfa, kisi, gecici);
      await admin.sayfa.reload();
      await expect(admin.sayfa.getByText(/kullanıcı şifre istedi/)).toHaveCount(0);
      await girisFormu(sayfa, kisi, gecici);
      await sayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
      await sifreDegistirFormu(sayfa, gecici, yeni);
      await sayfa.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
        timeout: 20_000,
      });
    },
  );
  await baglam.close();
});

test('Şifre değiştirme: "Diğer oturumlarımı kapat" seçilince diğer oturumlar gerçekten kapanır', async ({
  giris,
  browser,
}) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH|DELETE) \/api\/v1\/(master-data|auth)/);
  const kisi = `oturum.${damga().slice(-6).toLowerCase()}`;
  const ad = 'Robot Oturum';
  const ilk = `${sifre}-ilk`;
  const kendi = `${sifre}-kendi`;
  const ikinci = `${sifre}-ikinci`;
  const ucuncu = `${sifre}-ucuncu`;
  await depoHesabiAc(admin.sayfa, kisi, ad, ilk);

  const baglamA = await browser.newContext({ locale: 'tr-TR' });
  const a = await baglamA.newPage();
  await ilkGirisVeSifreSec(a, kisi, ilk, kendi);
  const baglamB = await browser.newContext({ locale: 'tr-TR' });
  const b = await baglamB.newPage();
  await girisFormu(b, kisi, kendi);
  await b.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), { timeout: 20_000 });

  const sifreyiDegistir = async (mevcut: string, yeni: string, diger: 'kapat' | 'koru') => {
    await a.getByRole('button', { name: ad }).click();
    await a.getByRole('menuitem', { name: 'Şifremi değiştir' }).click();
    await a.waitForURL(/sifre-degistir/);
    const kutu = a.getByRole('checkbox', { name: /Diğer oturumlarımı kapat/ });
    admin.kontrol('kutu varsayılan olarak işaretli gelmeli', await kutu.isChecked());
    if (diger === 'koru') await kutu.uncheck();
    await sifreDegistirFormu(a, mevcut, yeni);
    await a.waitForURL((adres) => !/giris|sifre-degistir/.test(adres.pathname), {
      timeout: 20_000,
    });
  };

  await admin.adim('kutu kapalıyken değiştirince diğer oturum açık kalır', async () => {
    await sifreyiDegistir(kendi, ikinci, 'koru');
    admin.kontrol('diğer oturum hâlâ açık olmalı (200)', (await oturumDurumu(b)) === 200);
    admin.kontrol('değiştiren oturum açık kalmalı (200)', (await oturumDurumu(a)) === 200);
  });

  await admin.adim(
    'kutu açıkken değiştirince diğer oturum kapanır, kendi oturumu açık kalır',
    async () => {
      await sifreyiDegistir(ikinci, ucuncu, 'kapat');
      admin.kontrol('diğer oturum kapanmalı (401)', (await oturumDurumu(b)) === 401);
      admin.kontrol('değiştiren oturum açık kalmalı (200)', (await oturumDurumu(a)) === 200);
    },
  );

  await baglamA.close();
  await baglamB.close();
});
