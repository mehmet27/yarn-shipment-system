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
    await mudur.sayfa.getByLabel(/Bitiş tarihi/).fill('2026-11-01');
    mudur.kontrol(
      'başlangıç bitişten sonraysa uyarı çıkmalı',
      /Başlangıç, bitişten sonra olamaz/.test(await mudur.sayfa.innerText('main')),
    );
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
      await alan.fill('Örnek Tekstil A.Ş.;ROBOT-açılış;5');
      await mudur.sayfa.getByRole('button', { name: /satırı yükle/ }).click();
      await expect(mudur.sayfa.getByText(/satır yüklendi/).first()).toBeVisible();
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
