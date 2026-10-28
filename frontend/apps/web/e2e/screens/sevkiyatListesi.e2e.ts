import { pick } from '../support/form';
import { expect, test } from '../support/test';

test.describe('Sevkiyat listesi', { tag: '@panel' }, () => {
  test('depo listeyi görür ve satırdan sevkiyat dosyasını açar', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat');
    await page.getByRole('link', { name: 'SVK-2026-0003' }).click();
    await expect(page).toHaveURL(/\/panel\/sevkiyat\/shp-0003$/);
    await expect(page.getByRole('heading', { name: 'SVK-2026-0003' })).toBeVisible();
  });

  test('durum filtresi listeyi daraltır ve adrese yazılır', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat');
    await pick(page, 'Durum', 'Yolda');
    await expect(page).toHaveURL(/durum=IN_TRANSIT/);
    await expect(page.getByRole('link', { name: 'SVK-2026-0003' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toHaveCount(0);
  });

  test('yönetici (müdür) yeni sevkiyat butonunu görmez', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat');
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Yeni sevkiyat' })).toHaveCount(0);
  });
});
