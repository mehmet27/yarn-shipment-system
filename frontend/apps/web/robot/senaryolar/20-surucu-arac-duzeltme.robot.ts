import { expect, test } from '../support/robot';
import { taslakAc } from '../support/akis';
import { damga, sec } from '../support/yardimci';

/**
 * Şoför ve araç: fasoncu seçilince şoför listesine merkez şoförleri de gelir; müdür açık sevkiyatta şoför/aracı
 * düzeltir (depo düzeltemez); form uyarısı alan düzelince "Taslak oluştur"a basmadan kalkar.
 */
test('Şoför ve araç: merkez şoförleri listede, müdür düzeltir, uyarı anında kalkar', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);

  await mudur.adim(
    'fasoncu seçilince şoför listesinde "Merkez şoförleri" başlığı da görünür',
    async () => {
      await mudur.sayfa.goto('/panel/sevkiyat/yeni');
      await sec(mudur.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
      await mudur.sayfa
        .getByLabel(/^Şoför/)
        .first()
        .click();
      await expect(mudur.sayfa.getByText('Merkez şoförleri', { exact: true })).toBeVisible();
    },
  );

  await mudur.adim('uyarı alan düzelince "Taslak oluştur"a basmadan kalkar', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    await expect(mudur.sayfa.getByText('Fasoncu seçin.')).toBeVisible();
    await sec(mudur.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
    await expect(mudur.sayfa.getByText('Fasoncu seçin.')).toHaveCount(0);
  });

  const id = await taslakAc(mudur, damga(), '100');

  await mudur.adim('müdür taslakta aracı değiştirir; dosyada yeni plaka görünür', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('button', { name: 'Şoför / araç değiştir' }).click();
    await expect(mudur.sayfa.getByRole('button', { name: 'Kaydet' })).toBeDisabled();
    await mudur.sayfa
      .getByLabel(/Yeni araç/)
      .first()
      .click();
    const secenek = mudur.sayfa.locator('[role=option]:visible').first();
    const plaka = (await secenek.innerText()).trim();
    await secenek.click();
    await mudur.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(mudur.sayfa.getByRole('button', { name: 'Şoför / araç değiştir' })).toBeVisible();
    await expect(mudur.sayfa.locator('main')).toContainText(plaka);
  });

  await depo.adim('depo şoför/araç değiştiremez', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
    await depo.sayfa.getByText('Kilolar', { exact: true }).first().waitFor();
    await expect(depo.sayfa.getByRole('button', { name: 'Şoför / araç değiştir' })).toHaveCount(0);
  });
});
