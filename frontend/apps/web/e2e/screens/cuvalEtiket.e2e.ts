import { expect, test } from '../support/test';

test.describe('Çuval ve etiket', { tag: '@panel' }, () => {
  test(
    'depo partiye çuval üretir ve etiketlerini basar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('WAREHOUSE', '/panel/cuvallar');
      await page.getByRole('textbox', { name: 'Adet' }).fill('2');
      await page.getByRole('button', { name: 'Çuval oluştur' }).click();
      await expect(page.getByRole('status')).toContainText('2 çuval oluşturuldu');
      await expect(page.getByText('2 ambalaj seçili')).toBeVisible();
      await page.getByRole('button', { name: 'Etiketleri bas' }).click();
      await expect(page.getByRole('dialog', { name: 'Etiketler' })).toBeVisible();
    },
  );

  test('basılmış etiketi gerekçesiz yeniden basmaz', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/cuvallar');
    await page.getByRole('checkbox', { name: 'BAG-7K3M-Q9XD seç' }).check();
    await expect(page.getByRole('button', { name: 'Etiketleri bas' })).toBeDisabled();
    await page.getByLabel(/Yeniden baskı gerekçesi/).fill('Etiket yırtıldı');
    await expect(page.getByRole('button', { name: 'Etiketleri bas' })).toBeEnabled();
  });

  test('depo yanlış üretilen çuvalı gerekçeyle iptal eder', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/cuvallar');
    await page.getByRole('textbox', { name: 'Adet' }).fill('2');
    await page.getByRole('button', { name: 'Çuval oluştur' }).click();
    await expect(page.getByText('2 ambalaj seçili')).toBeVisible();
    await page.getByRole('button', { name: 'Seçilenleri iptal et' }).click();
    const dialog = page.getByRole('dialog', { name: 'Çuval iptali' });
    await dialog.getByLabel(/İptal gerekçesi/).fill('Yanlış sayıda üretildi');
    await dialog.getByRole('button', { name: '2 ambalajı iptal et' }).click();
    await expect(dialog).toBeHidden();
    await page.getByText('İptal', { exact: true }).first().click();
    await expect(page.getByRole('cell', { name: 'İptal' })).toHaveCount(2);
  });
});
