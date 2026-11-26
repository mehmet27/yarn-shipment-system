import {
  sevkiyatIpligiSec,
  iplikTanimla,
  kumasFormuDoldur,
  tamamlanmisSevkiyat,
} from '../support/akis';
import { expect, test } from '../support/robot';
import { damga } from '../support/yardimci';

/**
 * Kumaş karışımı: müdür Tanımlar'da kumaş cinsi için iplik karışımı tanımlar; kalite kumaş girerken cinsi yazınca
 * "Tanımlı karışımı yaz" ile oranlar hazır dolar, kontrolde hangi sevkiyattan kaç kilo düşeceği yazar, kayıttan sonra
 * sevkiyat dosyasında "Bu sevkiyatın ipliğinden üretilen kumaş" bölümü kumaşı gösterir.
 */
test('Kumaş karışımı: tanımla, kumaş girişinde oranları doldur, sevkiyat dosyasında üretilen kumaşı gör', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const fasonKalite = await giris('fason-kalite');
  const kalite = await giris('kalite');
  const iplik = damga();
  const kumas = `Karışım-${iplik}`;

  await mudur.adim('hazırlık: iplik tanımlanır, tamamlanmış sevkiyat açılır', async () => {
    await iplikTanimla(mudur, iplik);
  });
  const id = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100');

  await mudur.adim('Tanımlar > Kumaş karışımı: oranlar 100 olmadan kaydedilmez', async () => {
    await mudur.sayfa.goto('/panel/tanimlar');
    await mudur.sayfa.getByRole('tab', { name: 'Kumaş karışımı', exact: true }).click();
    await mudur.sayfa.getByRole('button', { name: 'Karışım ekle' }).click();
    await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill(kumas);
    await mudur.sayfa.getByRole('combobox', { name: 'İplik', exact: true }).click();
    await mudur.sayfa.getByRole('option', { name: iplik, exact: true }).click();
    await mudur.sayfa.getByLabel('Oran (%)').fill('60');
    await expect(mudur.sayfa.getByTestId('recipe-total')).toHaveText(
      'Toplam: %60 (tam %100 olmalı)',
    );
    await expect(mudur.sayfa.getByRole('button', { name: 'Kaydet' })).toBeDisabled();
    await mudur.sayfa.getByLabel('Oran (%)').fill('100');
    await expect(mudur.sayfa.getByTestId('recipe-total')).toHaveText('Toplam: %100');
    await mudur.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(mudur.sayfa.getByText(kumas).first()).toBeVisible();
  });

  await fasonKalite.adim(
    'kumaş girişi: cinsi yazınca tanımlı karışım oranları doldurur',
    async () => {
      await kumasFormuDoldur(fasonKalite, id, '90', `${iplik}-K`);
      await fasonKalite.sayfa.getByLabel(/Kumaş cinsi/).fill(kumas);
      await fasonKalite.sayfa.getByRole('button', { name: 'Tanımlı karışımı yaz' }).click();
      await expect(fasonKalite.sayfa.getByLabel(/^Oran: /).first()).toHaveValue('100');
      await expect(fasonKalite.sayfa.getByTestId('fabric-ratio-total')).toHaveText('Toplam: %100');
      await fasonKalite.sayfa.getByRole('button', { name: 'Kontrol et' }).click();
      await expect(fasonKalite.sayfa.getByText('Kumaş için şu iplikler düşülecek')).toBeVisible();
      await expect(fasonKalite.sayfa.getByText(/sevkiyatından 90 kg/)).toBeVisible();
      await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).click();
      await expect(fasonKalite.sayfa.getByText('Kumaş girişi kaydedildi.')).toBeVisible();
    },
  );

  await kalite.adim(
    'merkez kumaş girişi: cins seçilince iplik ve oran kendiliğinden gelir, cins değişince boşalır',
    async () => {
      await kalite.sayfa.goto('/panel/merkez-stok');
      await kalite.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
      await kalite.sayfa.getByRole('combobox', { name: /^Kumaş cinsi/ }).fill(kumas);
      await kalite.sayfa.getByLabel(/Toplam kumaş kg/).fill('10');
      await expect(kalite.sayfa.getByLabel('Oran (%) 1')).toHaveValue('100');
      await expect(kalite.sayfa.getByTestId('central-ratio-total')).toHaveText(
        'Oran toplamı: %100',
      );
      await expect(kalite.sayfa.getByText(/stoktan .* \(fire dahil\) · stokta/)).toBeVisible();
      await kalite.sayfa.getByRole('combobox', { name: /^Kumaş cinsi/ }).fill('Tanımsız kumaş');
      await expect(kalite.sayfa.getByLabel('Oran (%) 1')).toHaveValue('');
    },
  );

  await mudur.adim('yeni sevkiyat: kumaş cinsi tanımlı karışımlardan seçilir', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await sevkiyatIpligiSec(mudur.sayfa, iplik);
    await mudur.sayfa.getByRole('tab', { name: /Sipariş bilgileri/ }).click();
    await mudur.sayfa.getByRole('combobox', { name: 'Kumaş cinsi' }).click();
    await mudur.sayfa.getByRole('option', { name: kumas, exact: true }).click();
    await expect(mudur.sayfa.getByRole('combobox', { name: 'Kumaş cinsi' })).toHaveValue(kumas);
    await expect(
      mudur.sayfa.getByText('Seçili iplik bu kumaşın tanımlı karışımında yok.'),
    ).toHaveCount(0);
  });

  await mudur.adim('sevkiyat dosyası: üretilen kumaş iplikten görünür', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('tab', { name: 'Fire dosyası', exact: true }).click();
    await expect(mudur.sayfa.getByText('Bu sevkiyatın ipliğinden üretilen kumaş')).toBeVisible();
    await expect(mudur.sayfa.getByTestId('fabric-draws')).toContainText(`${iplik} → ${kumas}`);
    await expect(mudur.sayfa.getByTestId('fabric-draws')).toContainText('90 kg');
  });
});
