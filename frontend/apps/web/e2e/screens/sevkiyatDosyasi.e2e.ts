import { expect, test } from '../support/test';

test.describe('Sevkiyat dosyası', { tag: '@panel' }, () => {
  test('sekmeler arasında gezilir', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/sevkiyat/shp-0007');
    await expect(page.getByRole('cell', { name: '1.982,5 kg' })).toBeVisible();
    for (const tab of ['Çuvallar', 'Geçmiş', 'Kanıtlar']) {
      await page.getByRole('tab', { name: tab }).click();
      await expect(page.getByRole('tabpanel', { name: tab })).toBeVisible();
    }
  });

  // Mimari doküman zorunlu senaryosu: Yönetici rolünde hiç düzenleme butonu yoktur.
  test(
    'yönetici (müdür) taslak sevkiyatta hiçbir işlem butonu görmez',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('MANAGER', '/panel/sevkiyat/shp-0001');
      await expect(page.getByRole('heading', { name: 'SVK-2026-0001' })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Çıkış tartısını gir' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'İptal et' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Fotoğraf çek' })).toHaveCount(0);
    },
  );

  test('admin taslak sevkiyatı gerekçeyle iptal eder', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/sevkiyat/shp-0001');
    await page.getByRole('button', { name: 'İptal et' }).click();
    await page.getByLabel('İptal gerekçesi').fill('Müşteri siparişi geri çekti');
    await page.getByRole('button', { name: 'Sevkiyatı iptal et' }).click();
    await expect(page.getByText('İptal', { exact: true })).toBeVisible();
  });
});
