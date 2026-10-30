import { fillFields } from '../support/form';
import { expect, test } from '../support/test';
import { captureTicket } from '../support/ticket';

test.describe('Teslim tutanağı', { tag: '@saha' }, () => {
  test(
    'fasoncu adımları doldurup tutanağı gönderir; durum Kontrolde olur',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      const next = () => page.getByRole('button', { name: 'İleri' }).click();
      await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0004');
      await page.getByRole('link', { name: 'Teslim tutanağını doldur' }).click();

      await page.getByText('Sağlam', { exact: true }).click();
      await page.getByLabel('Mühür numarası').fill('M-0004');
      await next();

      const barcode = page.getByLabel('Çuval barkodu');
      for (const key of ['B-1', 'B-1', 'B-2']) {
        await barcode.fill(key);
        await barcode.press('Enter');
      }
      await expect(page.getByText('Okutulan çuval: 2')).toBeVisible();
      await next();

      await fillFields(page, {
        'Brüt (kg)': '1.995,25',
        'Dara (kg)': '17',
        'Tartılan çuval sayısı': '2',
      });
      await expect(page.getByText('Net: 1.978,25 kg')).toBeVisible();
      await captureTicket(page);
      await expect(page.getByText('Fiş okundu')).toBeVisible();
      await next();
      await next();

      await page.getByRole('button', { name: 'Tutanağı gönder' }).click();
      await expect(page.getByText('Kontrolde', { exact: true })).toBeVisible();
    },
  );

  // Kör giriş: fasoncu çıkış kilosunu ve mührünü hiçbir adımda görmez.
  test('çıkış tartısı ve mühür numarası görünmez', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0004/teslim');
    await expect(page.getByText(/Teslim tutanağı/).first()).toBeVisible();
    await expect(page.getByText('M-0004')).toHaveCount(0);
    await expect(page.getByText(/2\.000|1\.982/)).toHaveCount(0);
  });

  test('teslim beklemeyen sevkiyatta sihirbazı açmaz', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0007/teslim');
    await expect(page.getByText('Bu sevkiyat artık teslim tutanağı beklemiyor.')).toBeVisible();
  });

  // H-16: "En az bir çuval okutun." çuval okutulunca kaybolur.
  test('çuval okutulunca boş liste uyarısı kalkar', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0004/teslim');
    await page.getByText('Sağlam', { exact: true }).click();
    await page.getByLabel('Mühür numarası').fill('M-0004');
    await page.getByRole('button', { name: 'İleri' }).click();
    await page.getByRole('button', { name: 'İleri' }).click();
    await expect(page.getByText('En az bir çuval okutun.')).toBeVisible();
    const barcode = page.getByLabel('Çuval barkodu');
    await barcode.fill('B-1');
    await barcode.press('Enter');
    await expect(page.getByText('Okutulan çuval: 1')).toBeVisible();
    await expect(page.getByText('En az bir çuval okutun.')).toHaveCount(0);
  });
});
