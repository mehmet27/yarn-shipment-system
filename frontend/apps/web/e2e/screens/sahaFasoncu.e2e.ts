import { expect, test } from '../support/test';

test.describe('Saha: fasoncu', { tag: '@saha' }, () => {
  test('yalnızca kendi sevkiyatlarını görür', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha');
    await expect(page.getByRole('link', { name: 'SVK-2026-0001' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'SVK-2026-0002' })).toHaveCount(0);
  });

  test('yoldaki sevkiyatta durum görünür, irsaliye no görünmez', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0010');
    await expect(page.getByTestId('shipment-status')).toHaveText('Fasoncuya yolda');
    await expect(page.getByText('İrsaliye no')).toHaveCount(0);
  });
});
