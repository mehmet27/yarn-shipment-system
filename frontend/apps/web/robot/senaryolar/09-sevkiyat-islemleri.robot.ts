import {
  cuvalEkle,
  iplikTanimla,
  kumasGir,
  stokYetmeseDeTamamla,
  tamamlanmisSevkiyat,
  taslakAc,
  tartiGir,
} from '../support/akis';
import { GENEL_MESAJ, expect, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, resim, sec } from '../support/yardimci';

type Fire = {
  fireRate: string;
  status: string;
  fabricKg: string;
  faultyKg: string;
  actualFirePercent: string;
  invoicedKg: string;
  remainingKg: string;
};

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
    // İptalden sonra durum "İptal" olur ve iptal formu kaybolur; /iptal/i büyük İ ile eşleşmez.
    await expect(mudur.sayfa.locator('main')).toContainText('İptal edildi');
  });
});

test('Sevkiyat: merkez stok yetmiyorsa uyarı oluştururken bir kez çıkar, tamamlarken sorulmaz; eksik kadar sistem girişi yazılır', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  type Stok = {
    rows: { yarnType: string; stockKg: string }[];
    movements: { yarnType: string; kind: string; netKg: string; note?: string }[];
  };
  await mudur.adim("iplik Tanımlar'da var ama merkez stokta hiç yok", async () => {
    await iplikTanimla(mudur, iplik);
  });
  const id = await taslakAc(mudur, iplik, '100');
  await cuvalEkle(depo, id, 2, '50');
  await tartiGir(depo, id, '103,5', '3,5', '2');
  await depo.sayfa.waitForURL(new RegExp(`${id}$`), { timeout: 15_000 });
  await mudur.adim(
    'müdür: oluştururken stok uyarısını görür, kapatıp açınca yeniden görür',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
      const uyari = mudur.sayfa.getByTestId('stock-notice');
      await uyari.waitFor();
      const metin = await uyari.innerText();
      mudur.kontrol(
        'uyarı iplik adını ve eksik kiloyu söylemeli',
        metin.includes(iplik) && /100 kg/.test(metin),
        metin,
      );
      await uyari.getByRole('button', { name: 'Uyarıyı kapat' }).click();
      await expect(uyari).toHaveCount(0);
      await mudur.sayfa.reload();
      await mudur.sayfa.getByTestId('stock-notice').waitFor();
    },
  );
  await mudur.adim('müdür: tamamlarken stok sorusu çıkmaz, sevkiyat tamamlanır', async () => {
    await stokYetmeseDeTamamla(mudur, id, `${iplik}-IRS`);
  });
  await depo.adim('doğrula: stok eksiye düşmez (0 kg), sistem girişi iz bırakır', async () => {
    const stok = await depo.api<Stok>('/api/v1/central-stock');
    const satir = stok.rows.find((r) => r.yarnType === iplik);
    depo.kontrol(
      'iplik stoğu 0 kg olmalı, eksi olmamalı',
      Number(satir?.stockKg) === 0,
      satir?.stockKg,
    );
    const giris = stok.movements.find(
      (m) =>
        m.yarnType === iplik && m.kind === 'YARN_RECEIPT' && /Sistem girişi/.test(m.note ?? ''),
    );
    depo.kontrol(
      '100 kg sistem girişi yazılmış olmalı',
      Number(giris?.netKg) === 100,
      giris?.netKg,
    );
  });
});

test('Fire dosyası: oran değiştirme, fatura edildi ve kapatma, faturalanacaklar listesi', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const fasonKalite = await giris('fason-kalite');
  for (const r of [depo, mudur, fasonKalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  const y = { depo, mudur };
  const faturali = await tamamlanmisSevkiyat(y, iplik, '100', '3');
  const kapali = await tamamlanmisSevkiyat(y, `${iplik}-2`, '100', '3');

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
    await kumasGir(fasonKalite, faturali, '90', `${iplik}-K`);
    const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
    mudur.kontrol('hatalı kilo oluşmalı', Number(fire.faultyKg) > 0, fire.faultyKg);
    await mudur.sayfa.goto('/panel/faturalanacaklar');
    await expect(mudur.sayfa.locator('main')).toContainText(/SV-/);
  });

  const kartAc = async () => {
    const liste = await mudur.api<{ items: { shipmentId: string; shipmentNo: string }[] }>(
      '/api/v1/shipments?size=100',
    );
    const no = liste.items.find((s) => s.shipmentId === faturali)?.shipmentNo ?? '';
    await mudur.sayfa.goto('/panel/faturalanacaklar');
    const kart = mudur.sayfa.getByRole('article', { name: no });
    await kart.getByRole('button', { name: 'Fatura bilgisini gir' }).click();
    return kart;
  };

  await mudur.adim(
    'fatura edildi (parça): kalandan az kilo için fatura girilir, dosya açık kalır, kalan azalır',
    async () => {
      const once = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
      const kart = await kartAc();
      await kart.getByLabel(/Fatura no/).fill(`F-${iplik}-1`);
      await kart.getByLabel(/\(kg\)/).fill('1');
      await kart.getByRole('button', { name: 'Fatura edildi' }).click();
      await mudur.sayfa.waitForTimeout(1500);
      const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
      mudur.kontrol(
        'durum hâlâ TO_INVOICE, faturalanan 1 kg, kalan hatalı kilodan 1 kg az',
        fire.status === 'TO_INVOICE' &&
          Number(fire.invoicedKg) === 1 &&
          Math.abs(Number(fire.remainingKg) - (Number(fire.faultyKg) - 1)) < 0.001,
        JSON.stringify(fire),
      );
      mudur.kontrol(
        'gerçekleşen fire faturalanan kilo kadar düşer: (T − K − 1) / T',
        Number(fire.actualFirePercent) < Number(once.actualFirePercent),
        `${once.actualFirePercent} → ${fire.actualFirePercent}`,
      );
      await fasonKalite.sayfa.goto(`/panel/sevkiyat/${faturali}`);
      await fasonKalite.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }).click();
      await expect(fasonKalite.sayfa.getByTestId('fabric-limit-note')).toContainText(
        '1 kg için fatura kesildi',
      );
    },
  );

  await mudur.adim('fatura edildi (kalan): kalan kilo faturalanınca dosya kapanır', async () => {
    const kart = await kartAc();
    await kart.getByLabel(/Fatura no/).fill(`F-${iplik}-2`);
    await kart.getByRole('button', { name: 'Fatura edildi' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    const fire = await mudur.api<Fire>(`/api/v1/shipments/${faturali}/fire-settlement`);
    mudur.kontrol(
      'durum INVOICED, kalan 0',
      fire.status === 'INVOICED' && Number(fire.remainingKg) === 0,
      JSON.stringify(fire),
    );
  });

  await mudur.adim('hatalı kilo yoksa dosya kapatılır', async () => {
    await kumasGir(fasonKalite, kapali, '98', `${iplik}-K2`);
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
  for (const r of [depo, mudur, kalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  const id = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100', '3');

  await depo.adim(
    'iplik dönüşü (fasoncudan depoya): Yön alanı yok, gönderilenden fazlası reddedilir, makul değer kaydolur',
    async () => {
      await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
      await sekme(depo, 'İplik dönüşü');
      depo.kontrol(
        'iplik dönüşü formunda Yön seçimi olmamalı',
        (await depo.sayfa.getByLabel('Yön', { exact: true }).count()) === 0,
      );
      await depo.sayfa
        .getByLabel(/İplik cinsi/)
        .first()
        .fill(iplik);
      await depo.sayfa.getByLabel(/Net kilo/).fill('5000');
      await depo.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).click();
      const mesajlar = await depo.mesajlar();
      depo.kontrol(
        'fazla dönüş nedenini söylemeli',
        mesajlar.length > 0 && !mesajlar.some((m) => GENEL_MESAJ.test(m)),
        mesajlar.join(' | '),
      );
      await depo.sayfa.getByLabel(/Net kilo/).fill('5');
      await depo.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).click();
      await expect(depo.sayfa.locator('main')).toContainText(/Dönüş/);
    },
  );

  await mudur.adim(
    'iplik dönüşünü müdür kaydedemez: sekmede form ve kaydet düğmesi yok',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
      await sekme(mudur, 'İplik dönüşü');
      mudur.kontrol(
        'müdürde iplik dönüşü kaydet düğmesi olmamalı',
        (await mudur.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).count()) === 0,
      );
      await expect(mudur.sayfa.locator('main')).toContainText(/Dönüş/);
    },
  );

  await mudur.adim('düzeltme: müdür gerekçeyle çıkış tartısını düzeltir', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(mudur, 'Düzeltmeler');
    await mudur.sayfa.getByRole('button', { name: 'Düzeltmeyi kaydet' }).click();
    const bos = await mudur.mesajlar();
    mudur.kontrol('boş düzeltme uyarı vermeli', bos.length > 0);
    await sec(mudur.sayfa, /Ne düzeltilecek/, '');
    await mudur.sayfa.getByLabel(/Gerekçe/).fill('Robot: yanlış girildi');
    await mudur.sayfa.getByRole('button', { name: 'Düzeltmeyi kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1200);
  });

  await mudur.adim('kanıt geçersiz kıl', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(mudur, 'Kanıtlar');
    await mudur.sayfa.getByRole('button', { name: 'Geçersiz kıl' }).first().click();
    const onay = mudur.sayfa
      .getByRole('dialog')
      .getByRole('button', { name: /Geçersiz kıl|Onayla|Evet/ });
    if ((await onay.count()) > 0) {
      await mudur.sayfa
        .getByRole('dialog')
        .getByRole('textbox')
        .first()
        .fill('Robot: yanlış belge')
        .catch(() => undefined);
      await onay.last().click();
    }
    await mudur.sayfa.waitForTimeout(1200);
  });
});

test('Uyuşmazlık: itiraz, lab sonucu ve karar', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  for (const r of [depo, mudur, kalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const id = await tamamlanmisSevkiyat({ depo, mudur }, damga(), '100', '3');

  await kalite.adim('itiraz aç', async () => {
    await kalite.sayfa.goto(`/panel/sevkiyat/${id}`);
    await sekme(kalite, 'Kalite itirazı');
    await kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' }).click();
    const net = (
      await kalite.api<{ shipmentNetKg: string }>(`/api/v1/quality-claims/shipments/${id}`)
    ).shipmentNetKg;
    await expect(kalite.sayfa.getByText(/En çok [\d.,]+ kg yazabilirsiniz\./)).toBeVisible();
    await kalite.sayfa.getByLabel(/İtiraz edilen kilo/).fill(String(Math.ceil(Number(net)) + 1));
    await expect(
      kalite.sayfa.getByText(/Sevkiyatın net ağırlığı [\d.,]+ kg; bundan fazla yazamazsınız\./),
    ).toBeVisible();
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

  await kalite.adim(
    'ana sayfa: "Karar bekleyen uyuşmazlıklar" listesi (en çok 5, Tümünü gör)',
    async () => {
      await kalite.sayfa.goto('/panel');
      const kart = kalite.sayfa.getByTestId('pending-discrepancies');
      await expect(kart).toBeVisible();
      await expect(kart).toContainText(/Karar bekleyen uyuşmazlıklar: \d+/);
      kalite.kontrol(
        'listede en çok 5 uyuşmazlık bağlantısı olmalı',
        (await kart.locator('a[href*="/panel/uyusmazliklar/"]').count()) <= 5,
      );
      await expect(kart.getByRole('link', { name: 'Tümünü gör' })).toHaveAttribute(
        'href',
        '/panel/uyusmazliklar',
      );
      await kart.locator('a[href*="/panel/uyusmazliklar/"]').first().click();
      await expect(kalite.sayfa.getByRole('heading', { name: 'Karar ver' })).toBeVisible();
    },
  );

  await kalite.adim('lab sonucu girilir, ardından karar verilir', async () => {
    const no = (await kalite.api<{ shipmentNo: string }>(`/api/v1/shipments/${id}`)).shipmentNo;
    await kalite.sayfa.goto('/panel/uyusmazliklar');
    await kalite.sayfa.getByRole('link', { name: new RegExp(no) }).click();
    await kalite.sayfa.getByRole('button', { name: 'Bana ata' }).click();
    await expect(kalite.sayfa.getByText('Bu uyuşmazlık sizde.')).toBeVisible();
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
    await expect(kalite.sayfa.getByTestId('resolution-effect')).toContainText(
      /fasoncunun sorumlu olduğu iplikten düşülür/,
    );
    await kalite.sayfa.getByLabel(/^Not/).first().fill('Robot: haklı bulundu');
    await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
    await expect(kalite.sayfa.getByText(/Karar geri alınamaz\./)).toBeVisible();
    await kalite.sayfa.getByRole('button', { name: 'Vazgeç' }).click();
    await expect(kalite.sayfa.getByRole('button', { name: 'Karara bağla' })).toBeVisible();
    await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
    await kalite.sayfa.getByRole('button', { name: 'Evet, kararı kaydet' }).click();
    await expect(kalite.sayfa.locator('main')).toContainText(/ÇÖZÜLDÜ|Çözüldü/);
  });
});
