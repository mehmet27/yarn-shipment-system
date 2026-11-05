import { expect, test } from '../support/test';

test('sol üstteki marka ana sayfaya götürür @ekran', async ({ page, openAs }) => {
  await openAs('ADMIN', '/panel/raporlar');
  await expect(page).toHaveURL(/\/panel\/raporlar/);
  await page.getByRole('link', { name: 'İplik Sevkiyat' }).click();
  await expect(page).toHaveURL(/\/panel$/);
});
