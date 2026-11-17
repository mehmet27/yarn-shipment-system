import { expect, test } from '../support/test';

test.describe('Kantar uyuşmazlığı kararı', { tag: '@panel' }, () => {
  test('müdür üç karar kartını görür; iptal ayrı düğme olarak durmaz', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0045');
    const panel = page.getByTestId('weight-decision');
    await expect(panel.getByRole('radio')).toHaveCount(3);
    await expect(panel.getByRole('radio', { name: /İplik güncelle/ })).toBeVisible();
    await expect(page.getByRole('button', { name: 'İptal et' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Şoför / araç değiştir' })).toHaveCount(0);
  });

  test('"İplik güncelle": kilolar tutmadan kayıt reddedilir, tutunca sevkiyat yola çıkar', async ({
    page,
    openAs,
  }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0045');
    await page.getByRole('radio', { name: /İplik güncelle/ }).click();
    await page.getByRole('link', { name: 'İplik listesini düzelt' }).click();
    await expect(page.getByTestId('recipe-weight-status')).toContainText('Kilolar henüz tutmuyor');
    await page.getByLabel(/Gerekçe/).fill('Reçete düzeltildi');
    await page.getByRole('button', { name: 'Kaydet ve yola çıkar' }).click();
    await expect(page.getByTestId('recipe-error')).toContainText('Kilolar tartıyı tutmuyor');
    await page.getByLabel('Net kg 2').fill('470');
    await expect(page.getByTestId('recipe-weight-status')).toContainText('Kilolar tuttu');
    await page.getByRole('button', { name: 'Kaydet ve yola çıkar' }).click();
    await expect(page.getByText('Fasoncuya yolda').first()).toBeVisible();
  });

  test('taslakta merkez stok yetmiyorsa uyarı çıkar ve kapatılabilir', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0001');
    const notice = page.getByTestId('stock-notice');
    await expect(notice).toContainText('Merkez depoda iplik yetmiyor');
    await notice.getByRole('button', { name: 'Uyarıyı kapat' }).click();
    await expect(notice).toHaveCount(0);
  });
});
