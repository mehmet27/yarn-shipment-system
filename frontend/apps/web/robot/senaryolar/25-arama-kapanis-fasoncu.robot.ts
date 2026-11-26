import { taslakAc } from '../support/akis';
import { expect, rolTanimli, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, depoHesabiAc, girisFormu, istanbulGunu } from '../support/yardimci';

type Sevkiyat = { shipmentNo: string; subcontractorId: string; subcontractorTitle: string };
type Bakiye = { items: { subcontractorId: string }[]; totalKg: string };
type DenetimSayfasi = { page: { totalElements: number } };

const GIRIS_OLAYI = {
  yapildi: 'com.yarnshipment.auth.login-succeeded.v1',
  hatali: 'com.yarnshipment.auth.login-failed.v1',
} as const;

/** Düğmeye basar ve inen dosyanın adını döner. */
async function indir(robot: Robot, dugme: string): Promise<string> {
  const [indirme] = await Promise.all([
    robot.sayfa.waitForEvent('download', { timeout: 30_000 }),
    robot.sayfa.getByRole('button', { name: dugme }).first().click(),
  ]);
  return indirme.suggestedFilename();
}

/** Ekrandaki "7.930 kg" gibi kiloyu sayıya çevirir (binlik nokta, ondalık virgül). */
function kiloSayisi(metin: string): number {
  const bulunan = /(−|-)?([\d.]+(?:,\d+)?) kg/.exec(metin);
  if (bulunan === null) return Number.NaN;
  const sayi = Number((bulunan[2] ?? '').replaceAll('.', '').replace(',', '.'));
  return bulunan[1] === undefined ? sayi : -sayi;
}

/**
 * Sevkiyat arama: listede sevkiyat no ile arama, tarih aralığı uyarısı, süzgeçleri temizleme, süzgeçle Excel'e alma ve her
 * ekrandaki hızlı arama. Hızlı arama tek sonuçta doğrudan dosyayı açar.
 */
test('Yedi öneri: sevkiyat listesinde arama, tarih süzgeci, Excel ve her ekrandan hızlı arama', async ({
  giris,
}) => {
  const mudur = await giris('mudur');
  const iplik = damga();
  const id = await taslakAc(mudur, iplik, '100');
  const sevkiyat = await mudur.api<Sevkiyat>(`/api/v1/shipments/${id}`);

  await mudur.adim(
    'listede sevkiyat no aranır: adres "ara" taşır, yalnızca o sevkiyat kalır',
    async () => {
      await mudur.sayfa.goto('/panel/sevkiyat');
      const kutu = mudur.sayfa.getByLabel('Sevkiyat ara');
      await kutu.fill(sevkiyat.shipmentNo);
      await kutu.press('Enter');
      await expect(mudur.sayfa).toHaveURL(/[?&]ara=/);
      await expect(mudur.sayfa.getByRole('link', { name: sevkiyat.shipmentNo })).toBeVisible();
      // Süzülen liste sunucudan gelene kadar beklenir: başlık dışında tek satır kalmalı.
      await expect(mudur.sayfa.getByRole('table').getByRole('row')).toHaveCount(2);
    },
  );

  await mudur.adim('süzgeçle "Excel\'e al" sevkiyatlar.xlsx indirir', async () => {
    const dosya = await indir(mudur, "Excel'e al");
    mudur.kontrol('dosya adı sevkiyatlar.xlsx olmalı', dosya === 'sevkiyatlar.xlsx', dosya);
  });

  await mudur.adim(
    'uymayan aramada "Bu aramaya uyan sevkiyat yok." ve "Süzgeçleri temizle"',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat?ara=${encodeURIComponent(`yok-${iplik}`)}`);
      await expect(mudur.sayfa.getByText('Bu aramaya uyan sevkiyat yok.')).toBeVisible();
      await mudur.sayfa.getByRole('button', { name: 'Süzgeçleri temizle' }).click();
      await expect(mudur.sayfa).not.toHaveURL(/ara=/);
    },
  );

  await mudur.adim(
    'başlangıç bitişten sonraysa uyarı çıkar; geçerli aralık bugünkü sevkiyatı bulur',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat?bas=${istanbulGunu(1)}&bit=${istanbulGunu(-1)}`);
      await expect(
        mudur.sayfa.getByText('Başlangıç tarihi bitiş tarihinden sonra olamaz.'),
      ).toBeVisible();
      await mudur.sayfa.goto(
        `/panel/sevkiyat?bas=${istanbulGunu()}&bit=${istanbulGunu()}&ara=${encodeURIComponent(sevkiyat.shipmentNo)}`,
      );
      await expect(mudur.sayfa.getByRole('link', { name: sevkiyat.shipmentNo })).toBeVisible();
    },
  );

  await mudur.adim(
    'hızlı arama: bakiye ekranından sevkiyat no yazılınca dosya açılır',
    async () => {
      await mudur.sayfa.goto('/panel/bakiye');
      const kutu = mudur.sayfa.locator('header').getByLabel('Hızlı arama');
      await kutu.fill(sevkiyat.shipmentNo);
      await kutu.press('Enter');
      await mudur.sayfa.waitForURL(new RegExp(`/panel/sevkiyat/${id}$`), { timeout: 15_000 });
    },
  );

  await mudur.adim('hızlı arama: birden çok sonuçta liste o aramayla açılır', async () => {
    const kutu = mudur.sayfa.locator('header').getByLabel('Hızlı arama');
    await kutu.fill('SVK');
    await kutu.press('Enter');
    await mudur.sayfa.waitForURL(/\/panel\/sevkiyat(\/[0-9a-f-]{30,}|\?ara=SVK)$/, {
      timeout: 15_000,
    });
  });
});

/** Yönetici sevkiyat göremez; hızlı arama kutusu ona hiç çizilmez. */
test('Yedi öneri: hızlı arama sevkiyat göremeyen yöneticide yoktur', async ({ giris }) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const admin = await giris('admin');
  await admin.adim('yönetici panelinde "Hızlı arama" kutusu yok', async () => {
    await admin.sayfa.goto('/panel/kullanicilar');
    await admin.sayfa.getByRole('heading', { level: 1 }).waitFor();
    await expect(admin.sayfa.getByLabel('Hızlı arama')).toHaveCount(0);
  });
});

/**
 * Raporlar: son görünüm hatırlanır (sekme ve dönem), Özet kartlarında önceki dönem kıyası yazar, ay sonu kapanışı tek PDF
 * indirir. Uyuşmazlıklar listesi süzgeçle Excel'e alınır ve süzgeç bir sonraki girişte hazır gelir.
 */
test('Yedi öneri: son görünüm hatırlanır, dönem kıyası yazar, ay sonu kapanışı ve uyuşmazlık Excel iner', async ({
  giris,
}) => {
  const mudur = await giris('mudur');

  await mudur.adim('Özet: toplam sevkiyat kartında "Önceki dönem" kıyası yazar', async () => {
    await mudur.sayfa.goto('/panel/raporlar?sekme=summary&gun=30');
    await expect(mudur.sayfa.getByTestId('stat-total')).toContainText('Önceki dönem:', {
      timeout: 15_000,
    });
    const metin = await mudur.sayfa.getByTestId('stat-total').innerText();
    mudur.kontrol(
      'kıyas "arttı", "azaldı", "değişmedi" ya da yalnızca önceki değer olmalı',
      /Önceki dönem: \d+( · (%\d+ (arttı|azaldı)|değişmedi))?/.test(metin),
      metin,
    );
    await expect(mudur.sayfa.getByTestId('stat-open')).not.toContainText('Önceki dönem');
  });

  await mudur.adim(
    'sekme ve dönem seçilir; ekrana yeniden girince aynı görünüm açılır',
    async () => {
      await mudur.sayfa.getByRole('tab', { name: 'Fire raporu', exact: true }).click();
      await expect(mudur.sayfa).toHaveURL(/sekme=fire/);
      await mudur.sayfa.getByText('7 gün', { exact: true }).click();
      await expect(mudur.sayfa).toHaveURL(/sekme=fire/);
      await expect(mudur.sayfa).toHaveURL(/gun=7/);
      await mudur.sayfa.goto('/panel/bakiye');
      await mudur.sayfa.goto('/panel/raporlar');
      await expect(mudur.sayfa).toHaveURL(/sekme=fire/);
      await expect(mudur.sayfa).toHaveURL(/gun=7/);
      await expect(
        mudur.sayfa.getByRole('tab', { name: 'Fire raporu', exact: true }),
      ).toHaveAttribute('aria-selected', 'true');
      // Sonraki senaryolar varsayılan görünümle başlasın.
      await mudur.sayfa.getByRole('tab', { name: 'Özet', exact: true }).click();
      await expect(mudur.sayfa).not.toHaveURL(/sekme=/);
      await mudur.sayfa.getByText('30 gün', { exact: true }).click();
      await expect(mudur.sayfa).not.toHaveURL(/gun=/);
    },
  );

  await mudur.adim('ay sonu kapanışı: ay seçilir, tek PDF iner', async () => {
    await expect(mudur.sayfa.getByRole('combobox', { name: 'Kapanış ayı' })).toHaveValue(
      /^\S+ \d{4}$/,
    );
    const dosya = await indir(mudur, 'Ay sonu kapanışı (PDF)');
    mudur.kontrol(
      'dosya adı ay-sonu-kapanis-YYYY-AA.pdf olmalı',
      /^ay-sonu-kapanis-\d{4}-\d{2}\.pdf$/.test(dosya),
      dosya,
    );
  });

  await mudur.adim("uyuşmazlıklar: süzgeç hatırlanır, liste Excel'e alınır", async () => {
    await mudur.sayfa.goto('/panel/uyusmazliklar?durum=RESOLVED');
    await mudur.sayfa.waitForLoadState('networkidle');
    const dosya = await indir(mudur, "Excel'e al");
    mudur.kontrol('dosya adı uyusmazliklar.xlsx olmalı', dosya === 'uyusmazliklar.xlsx', dosya);
    await mudur.sayfa.goto('/panel/bakiye');
    await mudur.sayfa.goto('/panel/uyusmazliklar');
    await expect(mudur.sayfa).toHaveURL(/durum=RESOLVED/);
    // Ana sayfadaki "Tümünü gör" ve zil bağlantısı süzgeçsiz açar: sayfa numarası hatırlananı ezer.
    await mudur.sayfa.goto('/panel/uyusmazliklar?sayfa=1');
    await expect(mudur.sayfa).not.toHaveURL(/durum=/);
    await mudur.sayfa.goto('/panel/uyusmazliklar?durum=OPEN');
    await mudur.sayfa.waitForLoadState('networkidle');
  });
});

/**
 * Fasoncu sayfası: bakiye tablosundaki ad sayfaya götürür; sayfadaki "Fasoncudaki iplik" o fasoncunun bakiye toplamıyla
 * aynıdır (çapraz kontrol). Bakiye göremeyen depo sayfayı açamaz.
 */
test('Yedi öneri: fasoncu sayfası bakiye ile tutarlıdır, depo açamaz', async ({ giris }) => {
  const mudur = await giris('mudur');
  const depo = await giris('depo');
  const iplik = damga();
  const id = await taslakAc(mudur, iplik, '100');
  const sevkiyat = await mudur.api<Sevkiyat>(`/api/v1/shipments/${id}`);
  const yol = `/panel/bakiye/fasoncu/${sevkiyat.subcontractorId}`;

  await mudur.adim('bakiye tablosunda fasoncu adı bağlantıdır', async () => {
    await mudur.sayfa.goto('/panel/bakiye');
    await mudur.sayfa.waitForLoadState('networkidle');
    const tablo = mudur.sayfa.getByRole('table', { name: 'Fasoncu bakiyesi' });
    if ((await tablo.count()) === 0) return; // Temiz ortamda henüz bakiye hareketi yoktur.
    const ilk = tablo.getByRole('link').first();
    await expect(ilk).toHaveAttribute('href', /\/panel\/bakiye\/fasoncu\//);
  });

  await mudur.adim('fasoncu sayfası: ad başlıkta, kartlar ve dört liste görünür', async () => {
    await mudur.sayfa.goto(yol);
    await expect(
      mudur.sayfa.getByRole('heading', { level: 1, name: sevkiyat.subcontractorTitle }),
    ).toBeVisible({ timeout: 15_000 });
    for (const baslik of [
      'Yoldaki sevkiyatlar',
      'Kumaşı beklenenler',
      'Faturalanacaklar',
      'Açık uyuşmazlıklar',
    ])
      await expect(mudur.sayfa.getByText(baslik, { exact: true }).first()).toBeVisible();
    const metin = await mudur.sayfa.innerText('main');
    mudur.kontrol(
      'fasoncu sayfasında UUID görünmemeli',
      !/[0-9a-f]{8}-[0-9a-f]{4}-/.test(metin),
      metin.match(/[0-9a-f]{8}-[0-9a-f]{4}-\S*/)?.[0],
    );
  });

  await mudur.adim('"Fasoncudaki iplik" = bu fasoncunun bakiye toplamı', async () => {
    const bakiye = await mudur.api<Bakiye>(
      `/api/v1/balances?subcontractorId=${sevkiyat.subcontractorId}`,
    );
    await mudur.sayfa.waitForLoadState('networkidle');
    const yazan = kiloSayisi(await mudur.sayfa.getByTestId('sub-balance').innerText());
    mudur.kontrol(
      'karttaki kilo bakiye ucuyla aynı olmalı',
      Math.abs(yazan - Number(bakiye.totalKg)) < 0.001,
      `${String(yazan)} ≠ ${bakiye.totalKg}`,
    );
  });

  await mudur.adim('"Fasoncu bakiyesine dön" bakiye ekranına götürür', async () => {
    await mudur.sayfa.getByRole('link', { name: 'Fasoncu bakiyesine dön' }).click();
    await mudur.sayfa.waitForURL(/\/panel\/bakiye$/);
  });

  if (rolTanimli('genel-mudur')) {
    const genel = await giris('genel-mudur');
    await genel.adim('genel müdür: sevkiyat bölümleri görünmez, yetki hatası alınmaz', async () => {
      await genel.sayfa.goto(yol);
      await expect(genel.sayfa.getByText('Kumaşı beklenenler', { exact: true })).toBeVisible();
      await genel.sayfa.waitForLoadState('networkidle');
      await expect(genel.sayfa.getByTestId('sub-in-transit')).toHaveCount(0);
      await expect(genel.sayfa.getByText('Yoldaki sevkiyatlar', { exact: true })).toHaveCount(0);
    });
  }

  await depo.adim('depo fasoncu sayfasını açamaz (403)', async () => {
    depo.beklenen(/403 GET/);
    await depo.sayfa.goto(yol);
    await depo.sayfa.waitForURL(/\/403$/, { timeout: 15_000 });
  });
});

/**
 * Giriş kayıtları: başarılı giriş ve var olan hesaba yanlış şifre denemesi denetim kaydına yazılır, ekranda "Giriş"
 * süzgeciyle görünür. Olmayan giriş adıyla yapılan deneme yazılmaz (yazılan metin şifre olabilir).
 */
test('Yedi öneri: giriş kayıtları denetim kaydına düşer', async ({ giris, browser }) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const sifre = process.env.ROBOT_SIFRE ?? '';
  const admin = await giris('admin');
  const sayi = async (islem: string) =>
    (
      await admin.api<DenetimSayfasi>(
        `/api/v1/audit-log?action=${encodeURIComponent(islem)}&size=1`,
      )
    ).page.totalElements;
  const kisi = `giris.${damga().slice(-6).toLowerCase()}`;
  await depoHesabiAc(admin.sayfa, kisi, 'Robot Giriş', `${sifre}-ilk`);
  const once = { yapildi: await sayi(GIRIS_OLAYI.yapildi), hatali: await sayi(GIRIS_OLAYI.hatali) };

  const baglam = await browser.newContext({ locale: 'tr-TR' });
  const sayfa = await baglam.newPage();
  await admin.adim(
    'yanlış şifre, olmayan giriş adı ve doğru şifreyle birer deneme yapılır',
    async () => {
      for (const [ad, parola] of [
        [kisi, 'yanlis-sifre-1'],
        [`yok.${kisi}`, 'yanlis-sifre-2'],
      ] as const) {
        const yanit = sayfa.waitForResponse((y) => y.url().includes('/api/v1/auth/login'));
        await girisFormu(sayfa, ad, parola);
        await yanit;
        await sayfa.getByText('Kullanıcı adı ya da şifre hatalı.').waitFor();
      }
      await girisFormu(sayfa, kisi, `${sifre}-ilk`);
      await sayfa.waitForURL(/sifre-degistir/, { timeout: 20_000 });
    },
  );
  await baglam.close();

  await admin.adim(
    'kayıt: bir hatalı deneme ve bir giriş eklendi; olmayan ad yazılmadı',
    async () => {
      const hatali = await sayi(GIRIS_OLAYI.hatali);
      const yapildi = await sayi(GIRIS_OLAYI.yapildi);
      admin.kontrol(
        'hatalı deneme tam 1 artmalı (olmayan giriş adı sayılmaz)',
        hatali === once.hatali + 1,
        `${String(once.hatali)} → ${String(hatali)}`,
      );
      admin.kontrol(
        'başarılı giriş en az 1 artmalı',
        yapildi >= once.yapildi + 1,
        `${String(once.yapildi)} → ${String(yapildi)}`,
      );
    },
  );

  await admin.adim(
    'ekran: "Giriş" süzgeci kayıtları Türkçe adıyla ve kişinin adıyla gösterir',
    async () => {
      await admin.sayfa.goto('/panel/denetim');
      await admin.sayfa.getByRole('combobox', { name: 'Kayıt türü' }).click();
      await admin.sayfa.getByRole('option', { name: 'Giriş', exact: true }).click();
      await expect(admin.sayfa).toHaveURL(/varlik=LoginAccount/);
      await expect(admin.sayfa.getByText('Giriş yapıldı').first()).toBeVisible({ timeout: 15_000 });
      await expect(admin.sayfa.getByText('Hesaba yanlış şifre denendi').first()).toBeVisible();
      await expect(admin.sayfa.getByText('Robot Giriş').first()).toBeVisible();
      const metin = await admin.sayfa.innerText('main');
      admin.kontrol(
        'olay kodu ham görünmemeli',
        !/com\.yarnshipment/.test(metin),
        metin.match(/com\.yarnshipment\S*/)?.[0],
      );
    },
  );
});
