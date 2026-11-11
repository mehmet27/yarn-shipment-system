import type { Page } from '@playwright/test';
import { pick } from '../support/form';
import { expect, test } from '../support/test';

async function fillRecipe(page: Page) {
  await page.getByLabel(/ya da serbest metin/).fill('20 denye likra');
  await page.getByLabel(/Net kg 1/).fill('2.036');
}

test.describe('Yeni sevkiyat', { tag: '@panel' }, () => {
  test(
    'müdür taslak sevkiyat oluşturur ve dosyasına gider',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('MANAGER', '/panel/sevkiyat');
      await page.getByRole('link', { name: 'Yeni sevkiyat' }).click();
      await pick(page, /Fasoncu/, 'Bursa Tekstil');
      await pick(page, /Şoför/, 'Mert Kaya');
      await fillRecipe(page);
      await page.getByRole('button', { name: 'Taslak oluştur' }).click();
      await expect(page.getByRole('heading', { name: 'SVK-2026-0046' })).toBeVisible();
      await expect(page.getByTestId('shipment-status')).toHaveText('Taslak');
    },
  );

  test('fasoncu seçilmeden göndermeyi engeller', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/yeni');
    await page.getByRole('button', { name: 'Taslak oluştur' }).click();
    await expect(page.getByText('Fasoncu seçin.')).toBeVisible();
  });

  test('şoför listesi seçilen fasoncuya göre daralır', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/yeni');
    await pick(page, /Fasoncu/, 'Aydın Örme');
    await page.getByRole('combobox', { name: /Şoför/ }).click();
    await expect(page.getByRole('option', { name: 'Şahin Şoför' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Mert Kaya' })).toHaveCount(0);
  });
});
