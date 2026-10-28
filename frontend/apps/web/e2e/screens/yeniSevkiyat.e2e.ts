import { pick } from '../support/form';
import { expect, test } from '../support/test';

test.describe('Yeni sevkiyat', { tag: '@panel' }, () => {
  test(
    'depo taslak sevkiyat oluşturur ve dosyasına gider',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('WAREHOUSE', '/panel/sevkiyat');
      await page.getByRole('link', { name: 'Yeni sevkiyat' }).click();
      await pick(page, /Lokasyon/, 'Merkez Depo');
      await pick(page, /Fasoncu/, 'Bursa Tekstil');
      await pick(page, /Şoför/, 'Mert Kaya');
      await page.getByRole('button', { name: 'Taslak oluştur' }).click();
      await expect(page.getByRole('heading', { name: 'SVK-2026-0046' })).toBeVisible();
      await expect(page.getByText('Taslak', { exact: true })).toBeVisible();
    },
  );

  test('fasoncu seçilmeden göndermeyi engeller', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/yeni');
    await pick(page, /Lokasyon/, 'Merkez Depo');
    await page.getByRole('button', { name: 'Taslak oluştur' }).click();
    await expect(page.getByText('Fasoncu seçin.')).toBeVisible();
  });

  test('şoför listesi seçilen fasoncuya göre daralır', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/yeni');
    await pick(page, /Fasoncu/, 'Aydın Örme');
    await page.getByRole('combobox', { name: /Şoför/ }).click();
    await expect(page.getByRole('option', { name: 'Şahin Şoför' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Mert Kaya' })).toHaveCount(0);
  });
});
