import { fillFields } from '../support/form';
import { expect, test } from '../support/test';
import { captureTicket } from '../support/ticket';

const WEIGHING = { 'Brüt (kg)': '2.000', 'Dara (kg)': '17,5', 'Çuval sayısı': '40' };

test.describe('Çıkış tartısı', { tag: '@panel' }, () => {
  test(
    'depo tartı, mühür ve fişi kaydeder; sevkiyat Yüklendi olur',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
      await page.getByRole('link', { name: 'Çıkış tartısını gir' }).click();
      await fillFields(page, { ...WEIGHING, 'Mühür numarası': 'M-NEW' });
      await expect(page.getByText('Net: 1.982,5 kg')).toBeVisible();
      await captureTicket(page);
      await expect(page.getByText('Fiş okundu')).toBeVisible();
      await page.getByRole('button', { name: 'Kaydet' }).click();
      await expect(page.getByText('Yüklendi', { exact: true })).toBeVisible();
    },
  );

  test('başka sevkiyatta kullanılmış mührü reddeder', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, { ...WEIGHING, 'Mühür numarası': 'M-0002' });
    await captureTicket(page);
    await expect(page.getByText('Fiş okundu')).toBeVisible();
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(
      page.getByText('Bu mühür numarası başka bir sevkiyatta kullanılmış.'),
    ).toBeVisible();
  });

  test('fiş çekilmeden kaydetmez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, { ...WEIGHING, 'Mühür numarası': 'M-NEW' });
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(
      page.getByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeVisible();
  });

  test('çıkış yetkisi olmayan rol yetki sayfasına düşer', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0001/cikis');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
