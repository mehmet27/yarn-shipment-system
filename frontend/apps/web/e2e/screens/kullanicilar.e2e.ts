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

  test('yönetici fasoncu hesabını Kullanıcılar ekranından açar; SMS gider, hesap listede görünür', async ({
    page,
    openAs,
  }) => {
    await openAs('ADMIN', '/panel/kullanicilar');
    await page.getByRole('button', { name: 'Yeni kullanıcı' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('combobox', { name: /Rol/ }).click();
    await page.getByRole('option', { name: 'Fasoncu' }).click();
    await dialog.getByRole('combobox', { name: /Fasoncu kaydı/ }).click();
    await page.getByRole('option', { name: 'Bursa Tekstil' }).click();
    await dialog.getByLabel(/Giriş adı/).fill('bursa.tekstil');
    await dialog.getByLabel(/Telefon/).fill('0532 123 45 67');
    await dialog.getByRole('switch', { name: 'Merkez Depo' }).click();
    await dialog.getByRole('button', { name: /Hesabı aç/ }).click();
    await expect(dialog.getByText(/numarasına gönderildi/)).toBeVisible();
    await dialog.getByRole('button', { name: 'Tamam' }).click();

    await expect(page.getByRole('row', { name: /bursa\.tekstil/ })).toContainText('Fasoncu');
  });

  test('yönetici olmayan rol ekranı göremez', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/kullanicilar');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
