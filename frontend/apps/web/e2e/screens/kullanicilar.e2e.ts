import { expect, test } from '../support/test';

test.describe('Kullanıcılar', { tag: '@panel' }, () => {
  test(
    'yönetici giriş hesaplarını kullanıcı adıyla görür ve arar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('ADMIN', '/panel/kullanicilar');
      const table = page.getByRole('table');
      await expect(table.getByRole('row', { name: /Kalite Uzmanı/ })).toContainText(
        'İlk giriş bekleniyor',
      );
      await expect(table.getByRole('row', { name: /eski\.depo/ })).toContainText('Devre dışı');

      await page.getByRole('searchbox', { name: /Giriş adı, ad ya da e-posta/ }).fill('kalite');
      await expect(page.getByText('1 kullanıcı')).toBeVisible();
      await expect(table.getByText('Kalite Uzmanı')).toBeVisible();
    },
  );

  test('yönetici olmayan rol ekranı göremez', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/kullanicilar');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
