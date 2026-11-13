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

test('Tanımlar: yönetici her tanım türünde ekler, düzenler ve pasife alır', async ({ giris }) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH) \/api\/v1\/master-data/);
  const ek = damga().slice(-6);
  await admin.sayfa.goto('/panel/tanimlar');
  for (const t of TANIMLAR) {
    await admin.adim(`${t.sekme}: ekle, düzenle, pasife al`, async () => {
      await admin.sayfa.goto('/panel/tanimlar');
      await admin.sayfa.getByRole('tab', { name: t.sekme, exact: true }).click();
      await admin.sayfa.waitForTimeout(500);
      const sonEk = 'ek' in t ? ek.replace(/\D/g, '').padStart(4, '1').slice(-4) : `-${ek}`;
      const degerler = Object.entries(t.alanlar).map(([etiket, on]) => [etiket, `${on}${sonEk}`] as const);
      const aranan = degerler[degerler.length - 1]?.[1] ?? ek;
      // Plakada rakam sayısı sabit olduğu için yeni değer son hane değiştirilerek üretilir.
      const yeni = 'ek' in t ? `${aranan.slice(0, -1)}${aranan.endsWith('7') ? '8' : '7'}` : `${aranan}9`;
      // Boş kayıt reddedilir
      await admin.sayfa.getByRole('button', { name: 'Ekle' }).click();
      const pencere = admin.sayfa.getByRole('dialog');
      await pencere.getByRole('button', { name: 'Kaydet' }).click();
      admin.kontrol(`${t.sekme}: boş kayıt uyarı vermeli`, (await admin.mesajlar()).length > 0);
      for (const [etiket, deger] of degerler) await pencere.getByLabel(etiket, { exact: true }).fill(deger);
      await pencere.getByRole('button', { name: 'Kaydet' }).click();
      await ara(admin.sayfa, aranan);
      await expect(admin.sayfa.getByText(aranan).first()).toBeVisible();
      // Düzenle
      await satir(admin.sayfa, aranan, 'Düzenle').getByRole('button', { name: 'Düzenle' }).click();
      const duzen = admin.sayfa.getByRole('dialog');
      const ilkEtiket = degerler[degerler.length - 1]?.[0] ?? '';
      await duzen.getByLabel(ilkEtiket, { exact: true }).fill(yeni);
      await duzen.getByRole('button', { name: 'Kaydet' }).click();
      await ara(admin.sayfa, yeni);
      await expect(admin.sayfa.getByText(yeni).first()).toBeVisible();
      // Pasife al
      await satir(admin.sayfa, yeni, 'Pasife al').getByRole('button', { name: 'Pasife al' }).click();
      await admin.sayfa.waitForTimeout(800);
      const onay = admin.sayfa.getByRole('dialog').getByRole('button', { name: /Pasife al|Onayla|Evet/ });
      if ((await onay.count()) > 0) await onay.first().click();
      await expect(admin.sayfa.getByText(yeni)).toHaveCount(0);
      // Pasifleri göster anahtarı kaydı geri getirir
      await admin.sayfa.getByLabel('Pasifleri göster').click({ force: true });
      await expect(admin.sayfa.getByText(yeni).first()).toBeVisible();
      await admin.sayfa.getByLabel('Pasifleri göster').click({ force: true });
    });
  }
});

test('Partiler: yönetici parti ekler, düzenler, kalitesini kilitler, pasife alır', async ({ giris }) => {
  test.skip(!rolTanimli('admin'), 'admin için şifre tanımlı değil');
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT|PATCH) \/api\/v1\/master-data/);
  const no = `P-${damga().slice(-8)}`;
  await admin.sayfa.goto('/panel/partiler');
  await admin.adim('parti: boş kayıt reddedilir', async () => {
    await admin.sayfa.getByRole('button', { name: 'Ekle' }).click();
    await admin.sayfa.getByRole('dialog').getByRole('button', { name: 'Kaydet' }).click();
    admin.kontrol('boş parti uyarı vermeli', (await admin.mesajlar()).length > 0);
  });
  await admin.adim('parti: ekle', async () => {
    const pencere = admin.sayfa.getByRole('dialog');
    await pencere.getByLabel('Parti numarası *', { exact: true }).fill(no);
    await sec(admin.sayfa, /^İplik \*/, '', pencere);
    await sec(admin.sayfa, /^Kalite sınıfı/, '', pencere);
    await pencere.getByLabel(/Üretim tarihi/).fill('2026-11-01');
    await pencere.getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText(no).first()).toBeVisible();
  });
  await admin.adim('parti: düzenle', async () => {
    await satir(admin.sayfa, no, 'Düzenle').getByRole('button', { name: 'Düzenle' }).click();
    await admin.sayfa.getByRole('dialog').getByLabel('Parti numarası *', { exact: true }).fill(`${no}-d`);
    await admin.sayfa.getByRole('dialog').getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText(`${no}-d`).first()).toBeVisible();
  });
  await admin.adim('parti: kaliteyi kilitle (onaylı)', async () => {
    await satir(admin.sayfa, `${no}-d`, 'Kaliteyi kilitle').getByRole('button', { name: 'Kaliteyi kilitle' }).click();
    await admin.sayfa.getByRole('button', { name: /Evet|Onayla|Kilitle$/ }).last().click();
    await expect(satir(admin.sayfa, `${no}-d`, 'Pasife al')).toContainText(/kilitli/i);
  });
  await admin.adim('parti: pasife al', async () => {
    await satir(admin.sayfa, `${no}-d`, 'Pasife al').getByRole('button', { name: 'Pasife al' }).click();
    await admin.sayfa.waitForTimeout(800);
    const onay = admin.sayfa.getByRole('dialog').getByRole('button', { name: /Pasife al|Onayla|Evet/ });
    if ((await onay.count()) > 0) await onay.first().click();
    await expect(admin.sayfa.getByText(`${no}-d`)).toHaveCount(0);
  });
});
