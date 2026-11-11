import { expect, test } from '../support/test';

test.describe('Sevkiyat dosyası', { tag: '@panel' }, () => {
  test('sekmeler arasında gezilir', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/sevkiyat/shp-0007');
    await expect(page.getByRole('row', { name: /^Net/ }).getByText('1.982,5 kg')).toBeVisible();
    for (const tab of ['Çuvallar', 'Geçmiş', 'Kanıtlar']) {
      await page.getByRole('tab', { name: tab }).click();
      await expect(page.getByRole('tabpanel', { name: tab })).toBeVisible();
    }
  });

  // Müdür tartı girmez; taslağı iptal edebilir (kantar uyuşmazlığında yeni sevkiyat açılır).
  test(
    'müdür taslakta tartı girişi görmez, iptal düğmesini görür',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('MANAGER', '/panel/sevkiyat/shp-0001');
      await expect(page.getByRole('heading', { name: 'SVK-2026-0001' })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Çıkış tartısını gir' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'İptal et' })).toBeVisible();
    },
  );

  test(
    'müdür irsaliyeyi yükleyip sevkiyatı tamamlar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('MANAGER', '/panel/sevkiyat/shp-0002');
      const confirm = page.getByRole('button', { name: 'Sevkiyatı tamamla' });
      await expect(confirm).toBeDisabled();
      await page
        .getByTestId('upload-file-input')
        .first()
        .setInputFiles({
          name: 'irsaliye.pdf',
          mimeType: 'application/pdf',
          buffer: Buffer.from('%PDF-1.4 irsaliye'),
        });
      await expect(page.getByText('İrsaliye yüklendi')).toBeVisible();
      await page.getByLabel(/İrsaliye no/).fill('IRS-2026-77');
      await confirm.click();
      await expect(page.getByTestId('shipment-status')).toHaveText('Tamamlandı');
    },
  );

  test('admin taslak sevkiyatı gerekçeyle iptal eder', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/sevkiyat/shp-0001');
    await page.getByRole('button', { name: 'İptal et' }).click();
    await page.getByLabel('İptal gerekçesi').fill('Müşteri siparişi geri çekti');
    await page.getByRole('button', { name: 'Sevkiyatı iptal et' }).click();
    await expect(page.getByText('İptal', { exact: true })).toBeVisible();
  });

  test('depo tamamlanan sevkiyatta çıkış tartısını ve irsaliye numarasını görür', async ({
    page,
    openAs,
  }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0007');
    await expect(page.getByRole('row', { name: /^Net/ }).getByText('1.982,5 kg')).toBeVisible();
    await expect(page.getByText('IRS-0007')).toBeVisible();
  });

  // H-24: Geçmiş'te UUID değil; oturumdaki kullanıcı kendi adıyla görünür.
  test('geçmişte yapan kişi UUID yerine okunur adla görünür', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0007');
    await page.getByRole('tab', { name: 'Geçmiş' }).click();
    const history = page.getByRole('tabpanel', { name: 'Geçmiş' });
    await expect(history.getByText(/Deniz Depo/).first()).toBeVisible();
    await expect(history.getByText(/0{8}-|user-/)).toHaveCount(0);
  });
});
