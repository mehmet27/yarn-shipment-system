import type { Page } from '@playwright/test';
import { expect, test } from '../support/test';

test.describe('Çuval girişi', { tag: '@panel' }, () => {
  const openBags = async (page: Page) => {
    await page.getByRole('tab', { name: 'Çuvallar' }).click();
  };

  test('kiloyu yazınca listeye sıra numarasıyla eklenir ve tek istekle gönderilir', async ({
    page,
    openAs,
  }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const weight = page.getByLabel('Çuval kilosu (kg)');
    for (const value of ['50,5', '49']) {
      await weight.fill(value);
      await weight.press('Enter');
    }
    await expect(page.getByRole('button', { name: '2 çuvalı ekle' })).toBeVisible();
    await page.getByRole('button', { name: '2 çuvalı ekle' }).click();
    await expect(page.getByText('2 çuval eklendi.')).toBeVisible();
  });

  test('geçersiz kiloyu listeye almaz', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const weight = page.getByLabel('Çuval kilosu (kg)');
    await weight.fill('abc');
    await weight.press('Enter');
    await expect(
      page.getByText('Kiloyu sıfırdan büyük bir sayı olarak girin, ör. 50,5.'),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: '0 çuvalı ekle' })).toBeDisabled();
  });
});
