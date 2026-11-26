import { expect, rolTanimli, test } from '../support/robot';
import { logoPng } from '../support/yardimci';

test('Görünüm: yönetici firma adı ve logoyu kaydeder, varsayılana döner', async ({ giris }) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (PUT|POST|PATCH) /);
  await admin.sayfa.goto('/panel/gorunum');
  const ad = admin.sayfa.getByLabel(/Firma adı/);
  const eski = await ad.inputValue();
  await admin.adim('firma adı kaydet', async () => {
    await ad.fill('ROBOT Firma');
    await admin.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText('Görünüm kaydedildi.').first()).toBeVisible();
  });
  await admin.adim('hazır renk seçilir, "Değişiklikleri geri al" kayıtlı renge döner', async () => {
    const renk = admin.sayfa.getByLabel(/^Ana renk/);
    const kayitli = await renk.inputValue();
    await admin.sayfa.getByRole('button', { name: 'Bordo' }).click();
    admin.kontrol('hazır renk alana yazılmalı', (await renk.inputValue()) !== kayitli);
    await expect(admin.sayfa.getByText('Kaydedilmemiş değişiklik var.')).toBeVisible();
    await expect(admin.sayfa.getByLabel('Önizleme').getByText('Koyu görünüm')).toBeVisible();
    for (const ad of ['Lacivert', 'Yeşil', 'Petrol yeşili', 'Mor', 'Turuncu', 'Gri']) {
      const onceki = await renk.inputValue();
      await admin.sayfa.getByRole('button', { name: ad, exact: true }).click();
      admin.kontrol(`hazır renk "${ad}" alana yazılmalı`, (await renk.inputValue()) !== onceki);
    }
    await admin.sayfa.getByRole('button', { name: 'Değişiklikleri geri al' }).click();
    await expect(renk).toHaveValue(kayitli);
  });
  await admin.adim('logo seç: büyük dosya reddedilir', async () => {
    await admin.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'buyuk.png',
        mimeType: 'image/png',
        buffer: Buffer.alloc(300 * 1024, 1),
      });
    await expect(admin.sayfa.getByText(/en çok 140 KB|en çok 200 KB/).first()).toBeVisible();
  });
  await admin.adim('logo seç: geçerli PNG kaydedilir', async () => {
    await admin.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'logo.png',
        mimeType: 'image/png',
        buffer: await logoPng(admin.sayfa),
      });
    await admin.sayfa.waitForTimeout(800);
    await admin.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText('Görünüm kaydedildi.').first()).toBeVisible();
  });
  await admin.adim('varsayılana dön ve kaydet (eski ad geri gelir)', async () => {
    await admin.sayfa.getByRole('button', { name: 'Varsayılana dön' }).click();
    await ad.fill(eski);
    await admin.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText('Görünüm kaydedildi.').first()).toBeVisible();
  });
});

test('Raporlar: müdür her sekmeyi, dönemi, tarihleri, logoyu ve açılış bakiyesini dener', async ({
  giris,
}) => {
  test.skip(!rolTanimli('mudur'), 'mudur için şifre tanımlı değil');
  const mudur = await giris('mudur');
  mudur.beklenen(/4\d\d (POST|PUT) /);
  await mudur.sayfa.goto('/panel/raporlar');
  await mudur.adim('dönem ve tarih aralığı', async () => {
    await mudur.sayfa.getByLabel(/Başlangıç tarihi/).fill('2026-11-10');
    // Seçim adrese yazılıp ekran yenilenmeden ikinci tarih girilirse ilki kaybolur.
    await expect(mudur.sayfa.getByLabel(/Başlangıç tarihi/)).toHaveValue('2026-11-10');
    await mudur.sayfa.getByLabel(/Bitiş tarihi/).fill('2026-11-01');
    const uyariVar = await mudur.sayfa
      .getByText('Başlangıç, bitişten sonra olamaz.')
      .waitFor({ timeout: 8_000 })
      .then(
        () => true,
        () => false,
      );
    mudur.kontrol('başlangıç bitişten sonraysa uyarı çıkmalı', uyariVar);
    await mudur.sayfa.getByLabel(/Bitiş tarihi/).fill('2026-11-13');
    await mudur.sayfa.waitForTimeout(800);
  });
  await mudur.adim('rapor logosu ekle ve PDF indir', async () => {
    await mudur.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'logo.png',
        mimeType: 'image/png',
        buffer: await logoPng(mudur.sayfa),
      });
    await mudur.sayfa.waitForTimeout(800);
    const [indirme] = await Promise.all([
      mudur.sayfa.waitForEvent('download', { timeout: 20_000 }),
      mudur.sayfa.getByRole('button', { name: 'PDF indir' }).click(),
    ]);
    mudur.kontrol('logolu PDF inmeli', indirme.suggestedFilename().endsWith('.pdf'));
  });
  for (const sekme of [
    'Özet',
    'Fasoncular',
    'Sevkiyat dökümü',
    'Fasoncu bakiye',
    'Bakiye matrisi',
    'Fire raporu',
    'Giden iplik ↔ gelen kumaş',
    'Faturalanacaklar',
  ]) {
    await mudur.adim(`sekme ${sekme}: Excel ve PDF`, async () => {
      await mudur.sayfa.getByRole('tab', { name: sekme, exact: true }).click();
      await mudur.sayfa.waitForTimeout(600);
      for (const tur of ['Excel indir (.xlsx)', 'PDF indir']) {
        const [indirme] = await Promise.all([
          mudur.sayfa.waitForEvent('download', { timeout: 20_000 }),
          mudur.sayfa.getByRole('button', { name: tur }).first().click(),
        ]);
        mudur.kontrol(`${sekme}: ${tur} dosya vermeli`, indirme.suggestedFilename() !== '');
      }
    });
  }
  await mudur.adim(
    'açılış bakiyesi: hatalı satır yüklemeyi kapatır, geçerli satır yüklenir',
    async () => {
      await mudur.sayfa.getByRole('tab', { name: 'Bakiye matrisi', exact: true }).click();
      const alan = mudur.sayfa.getByLabel(/Satırlar/);
      await alan.fill('Olmayan Fason;Pamuk;10');
      mudur.kontrol(
        'bilinmeyen fasoncu için yükleme düğmesi pasif',
        await mudur.sayfa.getByRole('button', { name: /satırı yükle/ }).isDisabled(),
      );
      await alan.fill('Örnek Tekstil A.Ş.;Tanımsız iplik;5');
      mudur.kontrol(
        "Tanımlar'da olmayan iplik için yükleme düğmesi pasif",
        await mudur.sayfa.getByRole('button', { name: /satırı yükle/ }).isDisabled(),
      );
      const iplikler = await mudur.api<{ items: { name: string }[] }>(
        '/api/v1/master-data/products?size=1',
      );
      await alan.fill(`Örnek Tekstil A.Ş.;${iplikler.items[0]?.name ?? ''};5`);
      await mudur.sayfa.getByRole('button', { name: /satırı yükle/ }).click();
      await expect(mudur.sayfa.getByText(/satır yüklendi/).first()).toBeVisible();
    },
  );
});

type Egilim = { month: string; sentKg: string; fabricKg: string; shipmentCount: number };
type Bakiye = { totalKg: string; items: { subcontractorTitle: string; balanceKg: string }[] };
type IplikKumas = { sentKg: string };

/** "34.032,5 kg" gibi ekran metnini sayıya çevirir. */
const sayi = (metin: string): number =>
  Number(
    metin
      .replace(/[^\d.,−-]/g, '')
      .replace(/\./g, '')
      .replace(',', '.')
      .replace('−', '-'),
  );

const yakin = (a: number, b: number): boolean => Math.abs(a - b) < 0.0005;

/**
 * Özet sekmesindeki grafikler: genel müdür bölümleri görür; veri varsa grafik çizilir ve grafikteki rakam aynı verinin
 * geldiği uçla (Fasoncu bakiyesi, Giden iplik ↔ gelen kumaş) aynıdır. Veri yoksa bölüm boş durum cümlesini gösterir.
 */
test('Raporlar: genel müdür Özet sekmesinde grafikleri görür, rakamlar tablolarla tutar', async ({
  giris,
}) => {
  test.skip(!rolTanimli('genel-mudur'), 'genel-mudur için şifre tanımlı değil');
  const genel = await giris('genel-mudur');
  await genel.sayfa.goto('/panel/raporlar');

  await genel.adim('grafik bölümlerinin başlıkları görünür', async () => {
    for (const baslik of [
      'Aylık giden iplik ve gelen kumaş',
      'Hangi fasoncuda ne kadar iplik var',
      'Fasoncuların fire oranı',
      'Faturalama',
      'Merkez depodaki iplikler',
    ]) {
      await expect(genel.sayfa.getByText(baslik, { exact: true }).first()).toBeVisible();
    }
    await expect(genel.sayfa.getByTestId('chart-shipment-status')).toBeVisible();
    await expect(genel.sayfa.getByTestId('chart-invoicing')).toBeVisible();
    for (const satir of ['Faturalanmayı bekleyen', 'Faturalanan']) {
      await expect(
        genel.sayfa.getByTestId('chart-invoicing').getByText(satir, { exact: true }),
      ).toBeVisible();
    }
  });

  await genel.adim('aylık eğilim: 6 ay gelir, grafik ve tablo sunucuyla aynı', async () => {
    const aylar = await genel.api<Egilim[]>('/api/v1/reports/monthly-trend');
    genel.kontrol('eğilim son 6 ayı vermeli', aylar.length === 6, String(aylar.length));
    const bolum = genel.sayfa.getByTestId('section-trend');
    if (aylar.every((ay) => ay.shipmentCount === 0)) {
      await expect(bolum.getByText('Son 6 ayda tamamlanan sevkiyat yok.')).toBeVisible();
      return;
    }
    await expect(genel.sayfa.getByTestId('chart-trend')).toBeVisible();
    const satirlar = genel.sayfa.getByTestId('trend-table').locator('tbody tr');
    await expect(satirlar).toHaveCount(aylar.length);
    const son = aylar.at(-1);
    if (son === undefined) return;
    const hucreler = await satirlar.last().locator('td').allInnerTexts();
    genel.kontrol(
      'son ayın giden ipliği sunucuyla aynı',
      yakin(sayi(hucreler[1] ?? ''), Number(son.sentKg)),
      `${hucreler[1] ?? ''} ≠ ${son.sentKg}`,
    );
    genel.kontrol(
      'son ayın gelen kumaşı sunucuyla aynı',
      yakin(sayi(hucreler[3] ?? ''), Number(son.fabricKg)),
      `${hucreler[3] ?? ''} ≠ ${son.fabricKg}`,
    );
    const ayBasi = `${son.month}-01`;
    const bugun = new Date().toISOString().slice(0, 10);
    const rapor = await genel.api<IplikKumas[]>(
      `/api/v1/reports/yarn-vs-fabric?from=${ayBasi}&to=${bugun}`,
    );
    const toplam = rapor.reduce((a, r) => a + Number(r.sentKg), 0);
    genel.kontrol(
      'son ay = "Giden iplik ↔ gelen kumaş" raporunun aynı aydaki toplamı',
      yakin(toplam, Number(son.sentKg)),
      `${String(toplam)} ≠ ${son.sentKg}`,
    );
  });

  await genel.adim('fasoncularda iplik çubukları Fasoncu bakiyesi ile aynı', async () => {
    const bakiye = await genel.api<Bakiye>('/api/v1/balances');
    const fasoncu = new Map<string, number>();
    for (const satir of bakiye.items) {
      fasoncu.set(
        satir.subcontractorTitle,
        (fasoncu.get(satir.subcontractorTitle) ?? 0) + Number(satir.balanceKg),
      );
    }
    const toplam = [...fasoncu.values()].reduce((a, b) => a + b, 0);
    genel.kontrol(
      'fasoncu toplamları = bakiye toplamı',
      yakin(toplam, Number(bakiye.totalKg)),
      `${String(toplam)} ≠ ${bakiye.totalKg}`,
    );
    if (fasoncu.size === 0) {
      await expect(genel.sayfa.getByText('Fasoncularda iplik yok.')).toBeVisible();
      return;
    }
    const cubuklar = genel.sayfa.getByTestId('chart-subcontractor-yarn').getByRole('listitem');
    await expect(cubuklar.first()).toBeVisible();
    const [ad, kg] = [...fasoncu.entries()].sort((a, b) => b[1] - a[1])[0] ?? ['', 0];
    const ilk = (await cubuklar.first().innerText()).replace(/\s+/g, ' ');
    genel.kontrol('en çok ipliği olan fasoncu en üstte', ilk.includes(ad), `${ilk} / ${ad}`);
    const yazan = sayi(/−?[\d.]+(?:,\d+)? kg/.exec(ilk)?.[0] ?? '');
    genel.kontrol('çubuktaki kilo bakiye ile aynı', yakin(yazan, kg), `${ilk} ≠ ${String(kg)}`);
  });

  await genel.adim(
    '"Giden iplik ↔ gelen kumaş" sekmesinde fire çubukları tabloyla durur',
    async () => {
      await genel.sayfa
        .getByRole('tab', { name: 'Giden iplik ↔ gelen kumaş', exact: true })
        .click();
      const rapor = await genel.api<IplikKumas[]>('/api/v1/reports/yarn-vs-fabric');
      if (rapor.length > 0) {
        await expect(genel.sayfa.getByTestId('chart-fire-detail')).toBeVisible();
      }
    },
  );
});

test('Merkez iplik stoku: özet kartları, arama, sıralama, tür süzgeci ve Excel/PDF indirme', async ({
  giris,
}) => {
  const depo = await giris('depo');
  await depo.sayfa.goto('/panel/merkez-stok');
  await depo.adim('özet kartları görünür', async () => {
    for (const ad of [
      'Toplam iplik stoku',
      'Üretilen kumaş',
      'Eksiye düşen iplik',
      'Gelen ipliğin dağılımı',
      'Toplam gelen iplik',
      'Merkez depoda',
      'Fasoncularda',
      'Kapıdan aktarılan',
    ]) {
      await expect(depo.sayfa.getByText(ad).first()).toBeVisible();
    }
  });
  await depo.adim('iplik cinsi ara ve kiloya göre sırala', async () => {
    const ara = depo.sayfa.getByLabel('İplik cinsi ara');
    if (await ara.isVisible()) {
      await ara.fill('ROBOT-olmayan-iplik');
      await expect(depo.sayfa.getByText('Aramaya uyan iplik yok.')).toBeVisible();
      await ara.fill('');
      await depo.sayfa.getByText('Kiloya göre', { exact: true }).click();
    }
  });
  await depo.adim('Excel ve PDF indir', async () => {
    for (const tur of ['Excel indir (.xlsx)', 'PDF indir']) {
      const [indirme] = await Promise.all([
        depo.sayfa.waitForEvent('download', { timeout: 20_000 }),
        depo.sayfa.getByRole('button', { name: tur }).click(),
      ]);
      depo.kontrol(`stok: ${tur} dosya vermeli`, indirme.suggestedFilename() !== '');
    }
  });
  await depo.adim('Yenile düğmesi stoğu yeniden yükler', async () => {
    await depo.sayfa.getByRole('button', { name: 'Yenile' }).click();
    await expect(depo.sayfa.getByText(/Son güncelleme:/)).toBeVisible();
  });
});
