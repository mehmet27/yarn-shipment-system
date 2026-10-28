import { pick } from '../support/form';
import { expect, test } from '../support/test';

test.describe('Uyuşmazlıklar', { tag: '@panel' }, () => {
  test('listeden detaya gider', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/uyusmazliklar');
    await page.getByRole('link', { name: 'UY-2026-0001' }).click();
    await expect(page.getByRole('heading', { name: 'UY-2026-0001' })).toBeVisible();
  });

  test('durum filtresini adrese yazar', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/uyusmazliklar');
    await pick(page, 'Durum', 'Çözüldü');
    await expect(page).toHaveURL(/durum=RESOLVED/);
  });

  test(
    'açık uyuşmazlığı bana atar ve notla karara bağlar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('ADMIN', '/panel/uyusmazliklar/disc-0');
      await page.getByRole('button', { name: 'Bana ata' }).click();
      await pick(page, /^Karar/, 'Tolerans içinde');
      await page.getByLabel(/^Not/).fill('Tolerans içinde kabul');
      await page.getByRole('button', { name: 'Karara bağla' }).click();
      await expect(page.getByText('Tolerans içinde kabul')).toHaveCount(2);
      await expect(page.getByRole('button', { name: 'Karara bağla' })).toHaveCount(0);
    },
  );
});
