import { cuvalEkle, taslakAc, tartiGir, tartiyiOnayla } from '../support/akis';
import { expect, test } from '../support/robot';
import { damga } from '../support/yardimci';

/**
 * Kantar uyuşmazlığında müdür "İplik güncelle" der: iplik listesi sayfası açılır, kilolar tartıyı tutmadan kayıt
 * alınmaz; tutunca sevkiyat yola çıkar. Uyuşmazlıktaki sevkiyatta şoför/araç değişmez (yalnızca taslakta değişir).
 */
test('İplik güncelle: kilolar tartıyı tutmadan yola çıkmaz, tutunca yola çıkar', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const id = await taslakAc(mudur, damga(), '100');
  await cuvalEkle(depo, id, 2, '50');

  await depo.adim(
    'çuval toplamının altında tartı: sevkiyat kantar uyuşmazlığına düşer',
    async () => {
      await tartiGir(depo, id, '1090,000', '1000,000', '2');
      await depo.sayfa.getByLabel(/Eksik kilo nedeni/).fill('Stok yetmedi');
      await depo.sayfa.getByRole('button', { name: 'İleri' }).click();
      await tartiyiOnayla(depo);
      await expect(
        depo.sayfa.getByText('Kantar uyuşmazlığı', { exact: true }).first(),
      ).toBeVisible();
    },
  );

  await mudur.adim('uyuşmazlıktaki sevkiyatta şoför/araç değiştirilemez', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByTestId('weight-decision').waitFor();
    await expect(mudur.sayfa.getByRole('button', { name: 'Şoför / araç değiştir' })).toHaveCount(0);
  });

  await mudur.adim('"İplik güncelle" seçilince iplik listesi sayfasına gider', async () => {
    await mudur.sayfa.getByRole('radio', { name: /İplik güncelle/ }).click();
    await mudur.sayfa.getByRole('link', { name: 'İplik listesini düzelt' }).click();
    await expect(mudur.sayfa.getByTestId('recipe-weight-status')).toContainText(
      'Kilolar henüz tutmuyor',
    );
  });

  await mudur.adim('kilolar tutmadan kayıt reddedilir ve sevkiyat yola çıkmaz', async () => {
    await mudur.sayfa.getByLabel(/Gerekçe/).fill('Reçete tartıya göre düzeltildi');
    await mudur.sayfa.getByRole('button', { name: 'Kaydet ve yola çıkar' }).click();
    await expect(mudur.sayfa.getByTestId('recipe-error')).toContainText('Kilolar tartıyı tutmuyor');
  });

  await mudur.adim(
    'kilo tartıya eşitlenince "Kilolar tuttu" yazar ve sevkiyat yola çıkar',
    async () => {
      const kilo = mudur.sayfa.getByLabel('Net kg 1');
      await kilo.fill('90');
      await expect(mudur.sayfa.getByTestId('recipe-weight-status')).toContainText(
        'Kilolar tuttu, sevkiyat yola çıkabilir',
      );
      await mudur.sayfa.getByRole('button', { name: 'Kaydet ve yola çıkar' }).click();
      await expect(mudur.sayfa.getByText('Fasoncuya yolda', { exact: true }).first()).toBeVisible();
    },
  );
});
