import { fillFields } from '../support/form';
import { expect, test } from '../support/test';
import { captureTicket } from '../support/ticket';

const WEIGHING = {
  'Dolu araç tartısı (brüt)': '2.000',
  'Boş araç tartısı (dara)': '17,5',
  'Çuval sayısı': '40',
};

test.describe('Çıkış tartısı', { tag: '@panel' }, () => {
  test(
    'depo tartı ve fişi kaydeder; sevkiyat Fasoncuya yolda olur',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
      await page.getByRole('link', { name: 'Çıkış tartısını gir' }).click();
      await fillFields(page, WEIGHING);
      await expect(page.getByText('Net: 1.982,5 kg')).toBeVisible();
      await captureTicket(page);
      await expect(
        page.getByText('Kantar fişi fotoğrafı kaydedildi, devam edebilirsiniz.'),
      ).toBeVisible();
      await page.getByRole('button', { name: 'Kaydet' }).click();
      await expect(page.getByTestId('shipment-status')).toHaveText('Fasoncuya yolda');
    },
  );

  test('fiş çekilmeden kaydetmez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, WEIGHING);
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(
      page.getByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeVisible();
  });

  test('fotoğraf seçilip gönderilmeden kaydetmez ve nedenini söyler', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, WEIGHING);
    await page.getByTestId('ticket-input').setInputFiles({
      name: 'fis.jpg',
      mimeType: 'image/jpeg',
      buffer: Buffer.alloc(2000),
    });
    await page.getByRole('button', { name: 'Kaydet' }).click();
    await expect(page.getByText(/henüz göndermediniz/)).toBeVisible();
  });

  test('çıkış yetkisi olmayan rol yetki sayfasına düşer', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0001/cikis');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });

  // H-20: fotoğraf yerine metin dosyası seçilince gönderme düğmesi çıkmaz, sebep yazılır.
  test('fiş yerine fotoğraf olmayan dosyayı kabul etmez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, WEIGHING);
    await page.getByTestId('ticket-input').setInputFiles({
      name: 'notlar.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('fiş değil'),
    });
    await expect(
      page.getByText('Kantar fişi için fotoğraf seçin (JPEG, PNG veya HEIC).'),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: 'Fotoğrafı gönder' })).toHaveCount(0);
  });
});
