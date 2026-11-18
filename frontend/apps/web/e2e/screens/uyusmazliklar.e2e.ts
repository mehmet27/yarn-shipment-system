import { pick } from '../support/form';
import { expect, test } from '../support/test';

test.describe('Uyuşmazlıklar', { tag: '@panel' }, () => {
  test('listeden detaya gider', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/uyusmazliklar');
    await page.getByRole('link', { name: 'UY-2026-0001' }).click();
    await expect(page.getByRole('heading', { name: 'UY-2026-0001' })).toBeVisible();
  });

  test('durum filtresini adrese yazar', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/uyusmazliklar');
    await pick(page, 'Durum', 'Çözüldü');
    await expect(page).toHaveURL(/durum=RESOLVED/);
  });

  test('kalite açık uyuşmazlığı bana atar ama karar veremez', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/uyusmazliklar/disc-0');
    await page.getByRole('button', { name: 'Bana ata' }).click();
    await expect(page.getByText('Bu uyuşmazlık sizde.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Karar ver' })).toHaveCount(0);
  });

  test(
    'müdür açık uyuşmazlığı notla karara bağlar',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('MANAGER', '/panel/uyusmazliklar/disc-0');
      await pick(page, /^Karar/, 'Haksız');
      await page.getByLabel(/^Not/).fill('Talep haksız kabul');
      await page.getByRole('button', { name: 'Karara bağla' }).click();
      await page.getByRole('button', { name: 'Evet, kararı kaydet' }).click();
      await expect(page.getByText('Talep haksız kabul')).toHaveCount(2);
      await expect(page.getByRole('button', { name: 'Karara bağla' })).toHaveCount(0);
    },
  );

  // H-22: kural kodu yerine Türkçe etiket görünür.
  test('detayda kural okunur metinle görünür', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/uyusmazliklar/disc-0');
    await expect(page.getByText('Kalite talebi').first()).toBeVisible();
    await expect(page.getByText('QUALITY_CLAIM')).toHaveCount(0);
    // Geçmişteki açılış kaydı da sistemi adıyla gösterir.
    await expect(page.getByText(/· Sistem$/)).toBeVisible();
  });

  // H-23: kartta özet boş kalmaz.
  test('kalite itirazı kartında özet boş kalmaz', async ({ page, openAs }) => {
    await openAs('QUALITY', '/panel/uyusmazliklar');
    await expect(page.getByRole('link', { name: 'UY-2026-0002' })).toContainText('Kalite talebi');
  });
});
