import { cuvalEkle, taslakAc, tartiGir, tartiyiOnayla } from '../support/akis';
import { expect, test } from '../support/robot';
import { damga } from '../support/yardimci';

/** Tartı çuvalların toplamının altındaysa depo nedenini yazar; müdür zilden görür, kararı depoya döner. */
test('Eksik kilo: neden zorunlu, müdür zilde görür, kararı depoya döner', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const id = await taslakAc(mudur, damga(), '100');
  await cuvalEkle(depo, id, 2, '50');

  await depo.adim('çuval toplamının altında tartı: neden yazılmadan kaydedilmez', async () => {
    await tartiGir(depo, id, '1090,000', '1000,000', '2');
    await expect(
      depo.sayfa.getByText('Tartılan net kilo beklenenin altında. Eksik kilonun nedenini yazın.'),
    ).toBeVisible();
  });

  await depo.adim(
    'neden yazılınca kayıt alınır ve sevkiyat kantar uyuşmazlığına düşer',
    async () => {
      await depo.sayfa.getByLabel(/Eksik kilo nedeni/).fill('Stok yetmedi');
      await depo.sayfa.getByRole('button', { name: 'İleri' }).click();
      await expect(depo.sayfa.getByText(/müdüre kantar uyuşmazlığı açılır/)).toBeVisible();
      await tartiyiOnayla(depo);
      await expect(
        depo.sayfa.getByText('Kantar uyuşmazlığı', { exact: true }).first(),
      ).toBeVisible();
    },
  );

  await mudur.adim('müdür zil simgesinde karar bekleyen sevkiyatı görür', async () => {
    await mudur.sayfa.goto('/panel');
    await mudur.sayfa.getByRole('button', { name: 'Bildirimler' }).click();
    await expect(
      mudur.sayfa.getByText('Kantar uyuşmazlığı: karar bekleyen sevkiyatlar'),
    ).toBeVisible();
  });

  await mudur.adim('müdür çuval ekle-çıkar der; depo isteği ve notu görür', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByText('Çuval ekle-çıkar', { exact: true }).click();
    await mudur.sayfa.getByLabel(/Gerekçe/).fill('Eksik çuvalı ekleyin');
    await mudur.sayfa.getByRole('button', { name: 'Kararı kaydet' }).click();
    await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
    await expect(depo.sayfa.getByTestId('manager-decision-note')).toContainText(
      'Eksik çuvalı ekleyin',
    );
  });
});
