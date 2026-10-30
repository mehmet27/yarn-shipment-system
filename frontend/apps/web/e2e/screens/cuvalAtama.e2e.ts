import type { Page } from '@playwright/test';
import { expect, test } from '../support/test';

test.describe('Çuval atama', { tag: '@panel' }, () => {
  const openBags = async (page: Page) => {
    await page.getByRole('tab', { name: 'Çuvallar' }).click();
  };

  // H-17: aynı anahtar büyük/küçük harf farkıyla iki kez listeye girmez.
  test('anahtarı büyük harfe çevirir, tekrarını eklemez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const key = page.getByLabel('Çuval anahtarı');
    for (const value of ['bag-0002', 'BAG-0002 ']) {
      await key.fill(value);
      await key.press('Enter');
    }
    await expect(page.getByRole('button', { name: '1 çuvalı ata' })).toBeVisible();
    await expect(page.getByText('BAG-0002', { exact: true })).toHaveCount(1);
  });

  // H-17: zaten atanmış çuval tekrar gönderilince "0 çuval atandı." yerine açıklama.
  test('zaten atanmış çuvalı yeniden atayınca yeni çuval olmadığını söyler', async ({
    page,
    openAs,
  }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const key = page.getByLabel('Çuval anahtarı');
    for (let round = 0; round < 2; round++) {
      await key.fill('BAG-0001');
      await key.press('Enter');
      await page.getByRole('button', { name: '1 çuvalı ata' }).click();
    }
    await expect(
      page.getByText('Bu çuvallar zaten bu sevkiyata atanmış; yeni çuval atanmadı.'),
    ).toBeVisible();
    await expect(page.getByText('0 çuval atandı.')).toHaveCount(0);
  });

  // H-03: başka lokasyonun çuvalı servis hatası gibi değil, kapsam mesajıyla reddedilir.
  test('başka lokasyonun çuvalını kapsam mesajıyla reddeder', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const key = page.getByLabel('Çuval anahtarı');
    await key.fill('BAG-0005');
    await key.press('Enter');
    await page.getByRole('button', { name: '1 çuvalı ata' }).click();
    await expect(
      page.getByText('Bir çuval bu sevkiyatın lokasyonuna ait değil; hiçbiri atanmadı.'),
    ).toBeVisible();
  });

  test('olmayan çuvalı bulunamadı mesajıyla reddeder', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await openBags(page);
    const key = page.getByLabel('Çuval anahtarı');
    await key.fill('BAG-9999');
    await key.press('Enter');
    await page.getByRole('button', { name: '1 çuvalı ata' }).click();
    await expect(page.getByText('Bir çuval bulunamadı; hiçbiri atanmadı.')).toBeVisible();
  });
});
