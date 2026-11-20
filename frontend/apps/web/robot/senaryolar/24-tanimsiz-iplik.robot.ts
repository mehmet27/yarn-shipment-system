import { fiyatYaz, iplikTanimla, tamamlanmisSevkiyat } from '../support/akis';
import { expect, test } from '../support/robot';
import { damga, sec } from '../support/yardimci';

type Stok = { rows: { yarnType: string; stockKg: string }[] };

/**
 * İplik yalnızca Tanımlar'dan seçilir: yeni sevkiyat formunda serbest metin alanı yoktur, Tanımlar'da olmayan iplikle taslak
 * açılamaz. Böylece merkez stok her iplikte izlenir ve eksiye düşmez: hiç girişi olmayan tanımlı iplikle merkez sevkiyatı
 * tamamlanınca eksik kadar sistem girişi yazılır, stok 0'da kalır.
 */
test("Tanımsız iplik: serbest metin yok, iplik Tanımlar'dan seçilir; merkez stok eksiye düşmez", async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const iplik = damga();
  const stok = async () => {
    const satir = (await mudur.api<Stok>('/api/v1/central-stock')).rows.find(
      (r) => r.yarnType === iplik,
    );
    return satir === undefined ? undefined : Number(satir.stockKg);
  };

  await mudur.adim(
    "yeni sevkiyat: serbest metin alanı yok, Tanımlar'da olmayan iplik seçilemez",
    async () => {
      await mudur.sayfa.goto('/panel/sevkiyat/yeni');
      await sec(mudur.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
      await expect(mudur.sayfa.getByLabel(/ya da listede yoksa yazın/i)).toHaveCount(0);
      const liste = mudur.sayfa.getByRole('combobox', { name: 'İplik cinsi' }).first();
      await liste.click();
      await liste.fill(iplik);
      await mudur.sayfa.waitForTimeout(1500);
      mudur.kontrol(
        "Tanımlar'da olmayan iplik listede çıkmamalı",
        (await mudur.sayfa.locator('[role=option]:visible').filter({ hasText: iplik }).count()) ===
          0,
      );
      await mudur.sayfa.keyboard.press('Escape');
      await mudur.sayfa.getByLabel(/Net kg 1/).fill('100');
      await fiyatYaz(mudur.sayfa);
      await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
      await expect(
        mudur.sayfa.locator(':text-matches("iplik cinsini listeden seçin"):visible').first(),
      ).toBeVisible();
      mudur.kontrol('iplik seçilmeden taslak açılmamalı', mudur.sayfa.url().endsWith('/yeni'));
    },
  );

  await mudur.adim(
    "iplik Tanımlar'a eklenir; girişi olmadan merkez sevkiyatı tamamlanır",
    async () => {
      await iplikTanimla(mudur, iplik);
      await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100');
    },
  );

  await mudur.adim('doğrula: merkez stok bu iplikte eksiye düşmedi, 0 kg', async () => {
    const kg = await stok();
    mudur.kontrol('merkez stok 0 kg (eksik kadar sistem girişi yazıldı)', kg === 0, String(kg));
  });
});
