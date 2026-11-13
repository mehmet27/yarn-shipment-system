import { tamamlanmisSevkiyat, taslakAc, cuvalEkle } from '../support/akis';
import { GENEL_MESAJ, expect, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, resim, sec } from '../support/yardimci';

type Fire = { fireRate: string; status: string; fabricKg: string; faultyKg: string };

async function sekme(robot: Robot, ad: string) {
  await robot.sayfa.getByRole('tab', { name: ad, exact: true }).click();
  await robot.sayfa.waitForTimeout(600);
}

test('Sevkiyat: çuval çıkarma ve taslak iptali', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|DELETE) /);
  const id = await taslakAc(mudur, damga(), '100');
  await depo.adim('çuval ekle ve birini sevkiyattan çıkar', async () => {
    await cuvalEkle(depo, id, 2, '50');
    await depo.sayfa.getByRole('button', { name: 'Sevkiyattan çıkar' }).first().click();
    await depo.sayfa.getByText('Sevkiyattaki çuvallar (1)').waitFor();
  });
  await mudur.adim('iptal: gerekçe yoksa düğme pasif, gerekçeyle iptal edilir', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('button', { name: 'İptal et' }).click();
    const gonder = mudur.sayfa.getByRole('button', { name: 'Sevkiyatı iptal et' });
    mudur.kontrol('gerekçesiz iptal düğmesi pasif', await gonder.isDisabled());
    await mudur.sayfa.getByLabel(/İptal gerekçesi/).fill('Robot iptali');
    await gonder.click();
    await expect(mudur.sayfa.locator('main')).toContainText(/iptal/i);
  });
});

test('Fire dosyası: oran değiştirme, fatura edildi ve kapatma, faturalanacaklar listesi', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  const y = { depo, mudur };
  const faturali = await tamamlanmisSevkiyat(y, iplik, '100', '3');
  const kapali = await tamamlanmisSevkiyat(y, `${iplik}-2`, '100', '3');

  const kumasGir = async (id: string, kg: string, no: string) => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(mudur, 'Kumaş girişi');
    await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
    await mudur.sayfa.getByLabel(/Kumaş net kilosu/).fill(kg);
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(no);
    await mudur.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1500);
  };

  await mudur.adim(
    'fire oranını değiştir: gerekçesiz reddedilir, gerekçeyle kaydolur',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${faturali}`);
      await sekme(mudur, 'Fire dosyası');
      await mudur.sayfa.getByLabel(/Fire oranı \(%\)/).fill('5');
      const kaydet = mudur.sayfa.getByRole('button', { name: 'Oranı kaydet' });
      if (!(await kaydet.isDisabled())) {
        await kaydet.click();
        const mesajlar = await mudur.mesajlar();
        mudur.kontrol(
          'gerekçesiz oran değişikliği nedenini söylemeli',
          mesajlar.length > 0 && !mesajlar.some((m) => GENEL_MESAJ.test(m)),
          mesajlar.join(' | '),
        );
      }
      await mudur.sayfa.getByLabel(/Değişiklik gerekçesi/).fill('Robot: anlaşma değişti');
      await mudur.sayfa.getByRole('button', { name: 'Oranı kaydet' }).click();
      await mudur.sayfa.waitForTimeout(1200);
      const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
      mudur.kontrol('fire oranı %5 olmalı', Number(fire.fireRate) === 0.05, fire.fireRate);
    },
  );

  await mudur.adim('eksik kumaş: dosya faturalanacaklar listesine düşer', async () => {
    await kumasGir(faturali, '90', `${iplik}-K`);
    const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
    mudur.kontrol('hatalı kilo oluşmalı', Number(fire.faultyKg) > 0, fire.faultyKg);
    await mudur.sayfa.goto('/panel/faturalanacaklar');
    await expect(mudur.sayfa.locator('main')).toContainText(/SV-/);
  });

  await mudur.adim('fatura edildi: fatura bilgisiyle kaydolur', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${faturali}`);
    await sekme(mudur, 'Fire dosyası');
    await mudur.sayfa.getByLabel(/Fatura no/).fill(`F-${iplik}`);
    await mudur.sayfa.getByRole('button', { name: 'Fatura edildi' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
    mudur.kontrol('durum INVOICED olmalı', fire.status === 'INVOICED', fire.status);
  });

  await mudur.adim('hatalı kilo yoksa dosya kapatılır', async () => {
    await kumasGir(kapali, '98', `${iplik}-K2`);
    await mudur.sayfa.goto(`/panel/sevkiyat/${kapali}`);
    await sekme(mudur, 'Fire dosyası');
    await mudur.sayfa.getByRole('button', { name: 'Kapat' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    const fire = await mudur.api<Fire>(`/api/v1/shipments/${kapali}/fire-settlement`);
    mudur.kontrol('durum CLOSED olmalı', fire.status === 'CLOSED', fire.status);
  });
});

test('İplik dönüşü, düzeltme ve kanıt geçersiz kılma', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  const admin = await giris('admin');
  for (const r of [depo, mudur, kalite, admin]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  const id = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100', '3');

  await mudur.adim(
    'iplik dönüşü: gönderilenden fazlası reddedilir, makul değer kaydolur',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
      await sekme(mudur, 'İplik dönüşü');
      await mudur.sayfa
        .getByLabel(/İplik cinsi/)
        .first()
        .fill(iplik);
      await mudur.sayfa.getByLabel(/Net kilo/).fill('5000');
      await mudur.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).click();
      const mesajlar = await mudur.mesajlar();
      mudur.kontrol(
        'fazla dönüş nedenini söylemeli',
        mesajlar.length > 0 && !mesajlar.some((m) => GENEL_MESAJ.test(m)),
        mesajlar.join(' | '),
      );
      await mudur.sayfa.getByLabel(/Net kilo/).fill('5');
      await mudur.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).click();
      await expect(mudur.sayfa.locator('main')).toContainText(/Dönüş/);
    },
  );

  await admin.adim('düzeltme: yönetici gerekçeyle çıkış tartısını düzeltir', async () => {
    await admin.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(admin, 'Düzeltmeler');
    await admin.sayfa.getByRole('button', { name: 'Düzeltmeyi kaydet' }).click();
    const bos = await admin.mesajlar();
    admin.kontrol('boş düzeltme uyarı vermeli', bos.length > 0);
    await sec(admin.sayfa, /Ne düzeltilecek/, '');
    await admin.sayfa.getByLabel(/Gerekçe/).fill('Robot: yanlış girildi');
    await admin.sayfa.getByRole('button', { name: 'Düzeltmeyi kaydet' }).click();
    await admin.sayfa.waitForTimeout(1200);
  });

  await admin.adim('kanıt geçersiz kıl', async () => {
    await admin.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(admin, 'Kanıtlar');
    await admin.sayfa.getByRole('button', { name: 'Geçersiz kıl' }).first().click();
    const onay = admin.sayfa
      .getByRole('dialog')
      .getByRole('button', { name: /Geçersiz kıl|Onayla|Evet/ });
    if ((await onay.count()) > 0) {
      await admin.sayfa
        .getByRole('dialog')
        .getByRole('textbox')
        .first()
        .fill('Robot: yanlış belge')
        .catch(() => undefined);
      await onay.last().click();
    }
    await admin.sayfa.waitForTimeout(1200);
  });
});

test('Uyuşmazlık: itiraz, lab sonucu, karar ve yönetici geçersiz kılma', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  for (const r of [depo, mudur, kalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const id = await tamamlanmisSevkiyat({ depo, mudur }, damga(), '100', '3');

  await kalite.adim('itiraz aç', async () => {
    await kalite.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(kalite, 'Kalite itirazı');
    await kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' }).click();
    await kalite.sayfa.getByLabel(/İtiraz edilen kilo/).fill('10');
    await kalite.sayfa.getByLabel(/Sorunu anlatın/).fill('Robot: leke');
    await kalite.sayfa
      .locator('input[type=file]')
      .last()
      .setInputFiles({
        name: 'leke.jpg',
        mimeType: 'image/jpeg',
        buffer: await resim(kalite.sayfa, 9),
      });
    await kalite.sayfa.getByText('1 fotoğraf eklendi.').waitFor();
    await kalite.sayfa.getByRole('button', { name: 'İtirazı gönder' }).click();
    await expect(kalite.sayfa.locator('main')).toContainText(/UY-SV-/);
  });

  await kalite.adim('lab sonucu girilir, ardından karar verilir', async () => {
    const no = (await kalite.api<{ shipmentNo: string }>(`/api/v1/shipments/${id}`)).shipmentNo;
    await kalite.sayfa.goto('/panel/uyusmazliklar');
    await kalite.sayfa.getByRole('link', { name: new RegExp(no) }).click();
    await kalite.sayfa.getByRole('button', { name: 'Bana ata' }).click();
    await kalite.sayfa.getByLabel(/Talep notu/).fill('Robot: numune');
    await kalite.sayfa.getByRole('button', { name: 'Kontrol iste' }).click();
    await kalite.sayfa.waitForTimeout(1200);
    await kalite.sayfa
      .getByLabel(/^Sonuç/)
      .first()
      .fill('Robot: leke doğrulandı');
    await kalite.sayfa
      .locator('input[type=file]')
      .last()
      .setInputFiles({
        name: 'lab.jpg',
        mimeType: 'image/jpeg',
        buffer: await resim(kalite.sayfa, 11),
      });
    await kalite.sayfa.waitForTimeout(1500);
    await kalite.sayfa.getByRole('button', { name: 'Sonucu kaydet' }).first().click();
    await kalite.sayfa.waitForTimeout(1200);
    await sec(kalite.sayfa, /^Karar/, 'Talep haklı');
    await kalite.sayfa.getByLabel(/^Not/).first().fill('Robot: haklı bulundu');
    await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
    await expect(kalite.sayfa.locator('main')).toContainText(/ÇÖZÜLDÜ|Çözüldü/);
  });
});
