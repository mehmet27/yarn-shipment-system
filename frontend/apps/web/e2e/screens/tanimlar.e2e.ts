import { expect, test } from '../support/test';

test.describe('Tanımlar', { tag: '@panel' }, () => {
  test('yönetici fasoncu ekler ve listede görür', { tag: '@smoke' }, async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Fasoncu ünvanı/).fill('Yeni Örme Ltd.');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByRole('group', { name: 'Yeni Örme Ltd.' })).toBeVisible();
  });

  // H-09: kayıttan sonra pencere sessizce kapanmaz, başarı bildirimi görünür.
  test('ekleme ve düzenlemeden sonra başarı bildirimi gösterir', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Fasoncu ünvanı/).fill('Yeni Örme Ltd.');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.getByRole('status')).toHaveText('Kayıt eklendi.');

    const card = page.getByRole('group', { name: 'Aydın Örme' });
    await card.getByRole('button', { name: 'Düzenle' }).click();
    await page.getByLabel(/Fasoncu ünvanı/).fill('Aydın Örme A.Ş.');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByRole('status')).toHaveText('Değişiklikler kaydedildi.');
    await page.getByRole('button', { name: 'Kapat' }).click();
    await expect(page.getByRole('status')).toHaveCount(0);
  });

  test('aynı plakayı reddeder', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('tab', { name: 'Araçlar' }).click();
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Plaka/).fill('34 abc 123');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByText('Bu plaka başka bir araçta kullanılıyor.')).toBeVisible();
  });

  // H-08: "abc" gibi plakalar sunucuya gönderilmeden alanın altında reddedilir.
  test('TR plaka desenine uymayan plakayı kaydetmez', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('tab', { name: 'Araçlar' }).click();
    await page.getByRole('button', { name: 'Ekle' }).click();
    const plate = page.getByLabel(/Plaka/);
    await plate.fill('abc');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByText('Geçerli bir plaka girin (ör. 34 ABC 123).')).toBeVisible();
    await expect(plate).toHaveAttribute('aria-invalid', 'true');

    await plate.fill('06 a 1234');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByRole('group', { name: '06A1234' })).toBeVisible();
  });

  // H-07: aynı ünvan (büyük/küçük harf farkı gözetilmeden) ikinci kez eklenmez.
  test('aynı fasoncu ünvanını reddeder', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByLabel(/Fasoncu ünvanı/).fill(' aydın örme ');
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByText('Bu ünvanla kayıtlı bir fasoncu zaten var.')).toBeVisible();
    await expect(page.getByRole('group', { name: 'Aydın Örme' })).toHaveCount(1);
  });

  // H-27: diyalog kapatma ve sayfalama okları ekran okuyucuya adıyla okunur.
  test('diyalog kapatma ve sayfalama düğmelerinin adı vardır', async ({ page, openAs }) => {
    await openAs('ADMIN', '/panel/tanimlar');
    await expect(page.getByRole('button', { name: 'Önceki sayfa' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sonraki sayfa' })).toBeVisible();
    await expect(page.getByRole('button', { name: '1', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Ekle' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Kapat' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });

  test('ana veri yetkisi olmayan rol giremez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/tanimlar');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
