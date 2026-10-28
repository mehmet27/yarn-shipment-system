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

  // H-22: kural kodu yerine Türkçe etiket, değerler TR sayı biçiminde.
  test('detayda kural okunur metinle, değerler kg biçiminde görünür', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/uyusmazliklar/disc-0');
    await expect(page.getByText('Net kilo tolerans dışında').first()).toBeVisible();
    await expect(page.getByText('2.000 kg', { exact: true })).toBeVisible();
    await expect(page.getByText('1.960 kg', { exact: true })).toBeVisible();
    await expect(page.getByText('QUANTITY_TOLERANCE')).toHaveCount(0);
    await expect(page.getByText(/2000\.000|1960\.000/)).toHaveCount(0);
    // Geçmişteki açılış kaydı da kuralı okunur yazar ve sistemi adıyla gösterir.
    await expect(page.getByText('Net kilo tolerans dışında')).toHaveCount(2);
    await expect(page.getByText(/· Sistem$/)).toBeVisible();
  });

  // H-23: kilo farkı olmayan türlerde de kartta özet vardır.
  test('çuval ve belge kartlarında özet boş kalmaz', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/uyusmazliklar');
    await expect(page.getByRole('link', { name: 'UY-2026-0002' })).toContainText(
      'Çuval sayısı tutmuyor · Beklenen 3, gerçek 2 · Vade:',
    );
    await expect(page.getByRole('link', { name: 'UY-2026-0005' })).toContainText(
      'İmzalı irsaliye yok · Vade:',
    );
  });
});
