import { taslakAc } from '../support/akis';
import { expect, rolTanimli, test } from '../support/robot';
import { damga, sec } from '../support/yardimci';

type Tutar = {
  shippedAmount: string;
  toInvoiceAmount: string;
  subcontractorYarnAmount: string;
  subcontractors: { amount: string }[];
};

const TL = new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Tutar bilgisi: birim fiyat yeni sevkiyatta zorunludur; tutarı yalnızca müdür ve genel müdür görür. Tutar her satırın
 * kendi fiyatıyla hesaplandığından kartlardaki değerler sunucunun verdiği toplamlarla ve fasoncu tablosuyla aynı olmalıdır.
 */
test('Tutar bilgisi: fiyat zorunlu; tutarı yalnızca müdür ve genel müdür görür; kartlar sunucuyla tutarlı', async ({
  giris,
}) => {
  test.skip(!rolTanimli('mudur') || !rolTanimli('genel-mudur'), 'mudur ya da genel-mudur yok');
  const mudur = await giris('mudur');
  const genel = await giris('genel-mudur');
  const kalite = await giris('kalite');
  mudur.beklenen(/4\d\d (POST|PUT|PATCH) /);
  kalite.beklenen(/403 GET/);

  await mudur.adim('fiyatsız sevkiyat açılamaz: fiyat alanı uyarı verir', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await sec(mudur.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
    await mudur.sayfa
      .getByLabel(/ya da listede yoksa yazın/i)
      .first()
      .fill(`${damga()}-fiyatsiz`);
    await mudur.sayfa.getByLabel(/Net kg 1/).fill('10');
    await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    await expect(mudur.sayfa.getByText(/Fiyat yazılmalı/)).toBeVisible();
    mudur.kontrol('sevkiyat oluşmamalı', mudur.sayfa.url().endsWith('/yeni'), mudur.sayfa.url());
  });

  await mudur.adim('fiyatlı sevkiyat açılır (fiyat sipariş bilgileri sekmesinde)', async () => {
    const id = await taslakAc(mudur, `${damga()}-tutar`, '10');
    mudur.kontrol('sevkiyat dosyası açılmalı', id.length > 0);
  });

  for (const rol of [mudur, genel]) {
    await rol.adim('raporlar Özet sekmesinde "Tutar bilgisi" ve üç kart görünür', async () => {
      await rol.sayfa.goto('/panel/raporlar');
      await expect(rol.sayfa.getByText('Tutar bilgisi', { exact: true })).toBeVisible();
      for (const kart of [
        'Fasoncudaki ipliğin değeri',
        'Faturalanacak tutar',
        'Yola çıkan ipliğin tutarı',
      ]) {
        await expect(rol.sayfa.getByText(kart, { exact: true })).toBeVisible();
      }
    });

    await rol.adim(
      'kartlardaki tutarlar sunucunun toplamlarıyla ve fasoncu tablosuyla aynı',
      async () => {
        const veri = await rol.api<Tutar>('/api/v1/reports/amounts');
        const toplam = veri.subcontractors.reduce((a, s) => a + Number(s.amount), 0);
        rol.kontrol(
          'fasoncudaki iplik değeri = fasoncu tablosundaki değerlerin toplamı',
          Math.abs(toplam - Number(veri.subcontractorYarnAmount)) < 0.005,
          `${String(toplam)} ≠ ${veri.subcontractorYarnAmount}`,
        );
        await expect(rol.sayfa.getByTestId('stat-amount-subcontractor-yarn')).toContainText(
          `${TL.format(Number(veri.subcontractorYarnAmount))} TL`,
        );
        await expect(rol.sayfa.getByTestId('stat-amount-to-invoice')).toContainText(
          `${TL.format(Number(veri.toInvoiceAmount))} TL`,
        );
        await expect(rol.sayfa.getByTestId('stat-amount-shipped')).toContainText(
          `${TL.format(Number(veri.shippedAmount))} TL`,
        );
      },
    );
  }

  await kalite.adim('kalite tutarı görmez: ekranda yok, API 403', async () => {
    await kalite.sayfa.goto('/panel/raporlar');
    await kalite.sayfa.waitForLoadState('networkidle');
    await expect(kalite.sayfa.getByText('Tutar bilgisi', { exact: true })).toHaveCount(0);
    const hata = await kalite.api('/api/v1/reports/amounts').then(
      () => 'yanıt geldi',
      (e: unknown) => String(e),
    );
    kalite.kontrol('tutar uç noktası kalite için 403 olmalı', /403 GET/.test(hata), hata);
  });
});
