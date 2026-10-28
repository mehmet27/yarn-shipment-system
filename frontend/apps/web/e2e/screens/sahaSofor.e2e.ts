import { expect, test } from '../support/test';

test.describe('Saha: şoför', { tag: '@saha' }, () => {
  test('yalnızca kendisine atanan sevkiyatları görür', async ({ page, openAs }) => {
    await openAs('DRIVER', '/saha');
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'SVK-2026-0002' })).toHaveCount(0);
  });

  test('çuval sayısını ve mührü görür, kiloyu görmez', async ({ page, openAs }) => {
    await openAs('DRIVER', '/saha/sevkiyat/shp-0010');
    await expect(page.getByText('M-0010')).toBeVisible();
    await expect(page.getByText('40', { exact: true })).toBeVisible();
    await expect(page.getByText(/kg/)).toHaveCount(0);
  });

  test(
    'yükü teslim alır, varışı beyan eder; durum Teslim bekliyor olur',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('DRIVER', '/saha/sevkiyat/shp-0010');
      await page.getByRole('button', { name: 'Yükü teslim aldım' }).click();
      await expect(page.getByText('Yolda', { exact: true })).toBeVisible();
      await page.getByRole('button', { name: 'Vardım, mühür sağlam' }).click();
      await expect(page.getByText('Teslim bekliyor', { exact: true })).toBeVisible();
    },
  );

  test('başkasının sevkiyatında yetki uyarısı görür', async ({ page, openAs }) => {
    await openAs('DRIVER', '/saha/sevkiyat/shp-0002');
    await expect(page.getByRole('alert')).toContainText('Bu sayfayı görme yetkiniz yok.');
  });
});
