import type { Page } from '@playwright/test';
import { expect, rolTanimli, test } from '../support/robot';
import { damga, satir, sec } from '../support/yardimci';

const TANIMLAR = [
  { sekme: 'Fasoncular', alanlar: { 'Fasoncu ünvanı *': 'Fason' } },
  { sekme: 'Şoförler', alanlar: { 'Ad soyad *': 'Şoför' } },
  { sekme: 'Araçlar', alanlar: { 'Plaka *': '34RBT' }, ek: 'sayi' },
  { sekme: 'İplikler', alanlar: { 'Kod *': 'RB', 'İplik adı *': 'İplik' } },
  { sekme: 'Kalite sınıfları', alanlar: { 'Kod *': 'RB', 'Kalite adı *': 'Kalite' } },
  { sekme: 'İplik tedarikçileri', alanlar: { 'Tedarikçi adı *': 'Tedarikçi' } },
] as const;

/** Liste sayfalı olduğu için kayıt, arama kutusuyla bulunur. */
async function ara(sayfa: Page, metin: string) {
  await sayfa.getByPlaceholder('Ara', { exact: true }).fill(metin);
  await sayfa.waitForTimeout(500);
}

test('Tanımlar: müdür her tanım türünde ekler, düzenler ve pasife alır', async ({ giris }) => {
  test.skip(!rolTanimli('mudur'), 'mudur için şifre tanımlı değil');
  const mudur = await giris('mudur');
  mudur.beklenen(/4\d\d (POST|PUT|PATCH) \/api\/v1\/master-data/);
  const ek = damga().slice(-6);
  await mudur.sayfa.goto('/panel/tanimlar');
  for (const t of TANIMLAR) {
    await mudur.adim(`${t.sekme}: ekle, düzenle, pasife al`, async () => {
      await mudur.sayfa.goto('/panel/tanimlar');
      await mudur.sayfa.getByRole('tab', { name: t.sekme, exact: true }).click();
      await mudur.sayfa.waitForTimeout(500);
      const sonEk = 'ek' in t ? ek.replace(/\D/g, '').padStart(4, '1').slice(-4) : `-${ek}`;
      const degerler = Object.entries(t.alanlar).map(
        ([etiket, on]) => [etiket, `${on}${sonEk}`] as const,
      );
      const aranan = degerler[degerler.length - 1]?.[1] ?? ek;
      // Plakada rakam sayısı sabit olduğu için yeni değer son hane değiştirilerek üretilir.
      const yeni =
        'ek' in t ? `${aranan.slice(0, -1)}${aranan.endsWith('7') ? '8' : '7'}` : `${aranan}9`;
      // Boş kayıt reddedilir
      await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
      const pencere = mudur.sayfa.getByRole('dialog');
      await pencere.getByRole('button', { name: 'Kaydet' }).click();
      mudur.kontrol(`${t.sekme}: boş kayıt uyarı vermeli`, (await mudur.mesajlar()).length > 0);
      for (const [etiket, deger] of degerler)
        await pencere.getByLabel(etiket, { exact: true }).fill(deger);
      await pencere.getByRole('button', { name: 'Kaydet' }).click();
      await ara(mudur.sayfa, aranan);
      await expect(mudur.sayfa.getByText(aranan).first()).toBeVisible();
      // Düzenle
      await satir(mudur.sayfa, aranan, 'Düzenle').getByRole('button', { name: 'Düzenle' }).click();
      const duzen = mudur.sayfa.getByRole('dialog');
      const ilkEtiket = degerler[degerler.length - 1]?.[0] ?? '';
      await duzen.getByLabel(ilkEtiket, { exact: true }).fill(yeni);
      await duzen.getByRole('button', { name: 'Kaydet' }).click();
      await ara(mudur.sayfa, yeni);
      await expect(mudur.sayfa.getByText(yeni).first()).toBeVisible();
      // Pasife al
      await satir(mudur.sayfa, yeni, 'Pasife al')
        .getByRole('button', { name: 'Pasife al' })
        .click();
      await mudur.sayfa.waitForTimeout(800);
      const onay = mudur.sayfa
        .getByRole('dialog')
        .getByRole('button', { name: /pasife al|Onayla|Evet/i });
      if ((await onay.count()) > 0) await onay.first().click();
      await expect(mudur.sayfa.getByText(yeni)).toHaveCount(0);
      // Pasifleri göster anahtarı kaydı geri getirir
      await mudur.sayfa.getByLabel('Pasifleri göster').click({ force: true });
      await expect(mudur.sayfa.getByText(yeni).first()).toBeVisible();
      await mudur.sayfa.getByLabel('Pasifleri göster').click({ force: true });
    });
  }
});

test('Partiler: müdür parti ekler, düzenler, kalitesini kilitler, pasife alır', async ({
  giris,
}) => {
  test.skip(!rolTanimli('mudur'), 'mudur için şifre tanımlı değil');
  const mudur = await giris('mudur');
  mudur.beklenen(/4\d\d (POST|PUT|PATCH) \/api\/v1\/master-data/);
  const no = `P-${damga().slice(-8)}`;
  // Lot seçicisi önbelleğe alınır; sayfa yenilenmeden yeni parti eklenince seçicide görünmelidir.
  await mudur.adim('parti: sevkiyat formunun lot seçicisi önbelleğe alınır', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await mudur.sayfa.getByLabel('Lot (isteğe bağlı)').first().click();
    await mudur.sayfa.keyboard.press('Escape');
    await mudur.sayfa.getByRole('link', { name: 'Partiler', exact: true }).click();
    await expect(mudur.sayfa).toHaveURL(/\/panel\/partiler/);
  });
  await mudur.adim('parti: boş kayıt reddedilir', async () => {
    await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
    await mudur.sayfa.getByRole('dialog').getByRole('button', { name: 'Kaydet' }).click();
    mudur.kontrol('boş parti uyarı vermeli', (await mudur.mesajlar()).length > 0);
  });
  await mudur.adim('parti: ekle', async () => {
    const pencere = mudur.sayfa.getByRole('dialog');
    await pencere.getByLabel('Parti numarası *', { exact: true }).fill(no);
    await sec(mudur.sayfa, /^İplik \*/, '', pencere);
    await sec(mudur.sayfa, /^Kalite sınıfı/, '', pencere);
    await pencere.getByLabel(/Üretim tarihi/).fill('2026-11-01');
    await pencere.getByRole('button', { name: 'Kaydet' }).click();
    await expect(mudur.sayfa.getByText(no).first()).toBeVisible();
  });
  await mudur.adim(
    'parti: yeni parti yenilemeden sevkiyat formunun lot seçicisinde görünür',
    async () => {
      await mudur.sayfa.getByRole('link', { name: 'Sevkiyatlar', exact: true }).click();
      await mudur.sayfa.getByRole('link', { name: 'Yeni sevkiyat' }).click();
      await mudur.sayfa.getByLabel('Lot (isteğe bağlı)').first().click();
      await expect(
        mudur.sayfa.locator('[role=option]:visible').filter({ hasText: no }).first(),
      ).toBeVisible();
      await mudur.sayfa.keyboard.press('Escape');
      await mudur.sayfa.getByRole('link', { name: 'Partiler', exact: true }).click();
      await expect(mudur.sayfa).toHaveURL(/\/panel\/partiler/);
    },
  );
  await mudur.adim('parti: düzenle', async () => {
    await satir(mudur.sayfa, no, 'Düzenle').getByRole('button', { name: 'Düzenle' }).click();
    await mudur.sayfa
      .getByRole('dialog')
      .getByLabel('Parti numarası *', { exact: true })
      .fill(`${no}-d`);
    await mudur.sayfa.getByRole('dialog').getByRole('button', { name: 'Kaydet' }).click();
    await expect(mudur.sayfa.getByText(`${no}-d`).first()).toBeVisible();
  });
  await mudur.adim('parti: kaliteyi kilitle (onaylı)', async () => {
    await satir(mudur.sayfa, `${no}-d`, 'Kaliteyi kilitle')
      .getByRole('button', { name: 'Kaliteyi kilitle' })
      .click();
    await mudur.sayfa
      .getByRole('button', { name: /Evet|Onayla|Kilitle$/ })
      .last()
      .click();
    await expect(satir(mudur.sayfa, `${no}-d`, 'Pasife al')).toContainText(/kilitli/i);
  });
  await mudur.adim('parti: pasife al', async () => {
    await satir(mudur.sayfa, `${no}-d`, 'Pasife al')
      .getByRole('button', { name: 'Pasife al' })
      .click();
    await mudur.sayfa.waitForTimeout(800);
    const onay = mudur.sayfa
      .getByRole('dialog')
      .getByRole('button', { name: /Pasife al|Onayla|Evet/ });
    if ((await onay.count()) > 0) await onay.first().click();
    await expect(mudur.sayfa.getByText(`${no}-d`)).toHaveCount(0);
  });
});
