import { expect, test } from '../support/test';

test.describe('Kullanıcılar', { tag: '@panel' }, () => {
  test(
    'yönetici giriş hesaplarını kullanıcı adıyla görür ve arar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('ADMIN', '/panel/kullanicilar');
      const table = page.getByRole('table');
      await expect(table.getByText('sofor', { exact: true })).toBeVisible();
      await expect(table.getByRole('row', { name: /sofor/ })).toContainText('İlk giriş bekleniyor');
      await expect(table.getByRole('row', { name: /eski\.depo/ })).toContainText('Devre dışı');

      await page.getByRole('searchbox', { name: /Giriş adı, ad ya da e-posta/ }).fill('kalite');
      await expect(page.getByText('1 kullanıcı')).toBeVisible();
      await expect(table.getByText('Kalite Uzmanı')).toBeVisible();
    },
  );

  test('Tanımlar ekranında açılan hesap listede görünür', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    const card = page.getByRole('group', { name: 'Bursa Tekstil' });
    await card.getByRole('button', { name: 'Giriş hesabı aç' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel(/Giriş adı/).fill('bursa.tekstil');
    await dialog.getByLabel(/E-posta/).fill('bilgi@bursa.com');
    await dialog.getByRole('switch', { name: 'Merkez Depo' }).click();
    await dialog.getByRole('button', { name: 'Hesabı aç' }).click();
    await expect(dialog.getByText('Giriş hesabı açıldı.')).toBeVisible();
    await dialog.getByRole('button', { name: 'Tamam' }).click();

    await page.getByRole('link', { name: 'Kullanıcılar' }).click();
    await expect(page.getByRole('row', { name: /bursa\.tekstil/ })).toContainText('Fasoncu');
  });

  test('yönetici olmayan rol ekranı göremez', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/kullanicilar');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
