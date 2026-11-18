import { addBags } from '../support/bags';
import { fillFields } from '../support/form';
import { expect, test } from '../support/test';
import { captureTicket } from '../support/ticket';

// Kayıt = tartı + fiş yükleme + yeniden okuma; yavaş koşuda 5 sn'yi aşabilir.
const SAVE_TIMEOUT_MS = 15_000;

const WEIGHING = {
  'Dolu araç tartısı (brüt)': '2.000',
  'Boş araç tartısı (dara)': '17,5',
  'Çuval sayısı': '2',
};

test.describe('Çıkış tartısı', { tag: '@panel' }, () => {
  test(
    'depo tartı ve fişi kaydeder; sevkiyat Fasoncuya yolda olur',
    { tag: '@smoke' },
    async ({ page, openAs }) => {
      await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
      await addBags(page);
      await page.getByRole('link', { name: 'Çıkış tartısını gir' }).click();
      await fillFields(page, WEIGHING);
      await expect(page.getByText('Net: 1.982,5 kg')).toBeVisible();
      await captureTicket(page);
      await expect(
        page.getByText('Fotoğraf seçildi. Çıkış tartısını kaydedince sisteme yüklenecek.'),
      ).toBeVisible();
      await page.getByRole('button', { name: 'İleri' }).click();
      await page
        .getByRole('dialog')
        .getByRole('button', { name: /^Onayla/ })
        .click();
      await expect(page.getByTestId('shipment-status')).toHaveText('Fasoncuya yolda', {
        timeout: SAVE_TIMEOUT_MS,
      });
    },
  );

  test('fiş çekilmeden kaydetmez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis');
    await fillFields(page, WEIGHING);
    await page.getByRole('button', { name: 'İleri' }).click();
    await expect(
      page.getByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeVisible();
  });

  test('fotoğraf, çıkış tartısı onaylanana kadar sisteme yüklenmez', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await addBags(page);
    await page.getByRole('link', { name: 'Çıkış tartısını gir' }).click();
    let uploads = 0;
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().endsWith('/api/v1/evidence/uploads')) {
        uploads += 1;
      }
    });
    await fillFields(page, WEIGHING);
    await captureTicket(page);
    await expect(
      page.getByText('Fotoğraf seçildi. Çıkış tartısını kaydedince sisteme yüklenecek.'),
    ).toBeVisible();
    expect(uploads).toBe(0);
    await page.getByRole('button', { name: 'İleri' }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    expect(uploads).toBe(0);
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /^Onayla/ })
      .click();
    await expect(page.getByTestId('shipment-status')).toHaveText('Fasoncuya yolda', {
      timeout: SAVE_TIMEOUT_MS,
    });
    expect(uploads).toBe(1);
  });

  test('çıkış yetkisi olmayan rol yetki sayfasına düşer', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0001/cikis');
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });

  // H-20: fotoğraf yerine metin dosyası seçilince fotoğraf seçildi bildirimi çıkmaz, sebep yazılır.
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
    await expect(page.getByText(/Fotoğraf seçildi/)).toHaveCount(0);
  });
});
