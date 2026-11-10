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
    await pick(page, 'Durum', 'Fasoncuya yolda');
    await expect(page).toHaveURL(/durum=AWAITING_DELIVERY/);
    await expect(page.getByRole('link', { name: 'SVK-2026-0002' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toHaveCount(0);
  });

  test('müdür yeni sevkiyat açabilir, kalite açamaz', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat');
    await expect(page.getByRole('link', { name: 'Yeni sevkiyat' })).toBeVisible();
    await openAs('QUALITY', '/panel/sevkiyat');
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Yeni sevkiyat' })).toHaveCount(0);
  });
});
