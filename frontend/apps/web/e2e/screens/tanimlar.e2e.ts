import { expect, test } from '../support/test';

test.describe('Tanımlar', { tag: '@panel' }, () => {
  test('yönetici fasoncu ekler ve listede görür', { tag: '@smoke' }, async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Fasoncu ünvanı/).fill('Yeni Örme Ltd.');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByRole('group', { name: 'Yeni Örme Ltd.' })).toBeVisible();
  });

  test('aynı plakayı reddeder', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('tab', { name: 'Araçlar' }).click();
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Plaka/).fill('34 abc 123');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByText('Bu plaka başka bir araçta kullanılıyor.')).toBeVisible();
  });

  test('ana veri yetkisi olmayan rol giremez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/tanimlar');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
