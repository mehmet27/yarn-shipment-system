import { fireHesapla, kiloEsit } from '../kurallar/hesap';
import { cuvalEkle, tamamlanmisSevkiyat, taslakAc } from '../support/akis';
import { expect, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, resim, sec } from '../support/yardimci';

type Bakiye = {
  items: { subcontractorTitle: string; yarnType: string; balanceKg: string }[];
  totalKg: string;
};
type Stok = { rows: { yarnType: string; stockKg: string }[] };
type Ozet = { shipments: Record<string, number> };
type Fire = {
  sentKg: string;
  returnedKg: string;
  claimKg: string;
  fabricKg: string;
  baseKg: string;
  expectedFabricKg: string;
  faultyKg: string;
  fireRate: string;
};

const BASKA = 'Başka Dokuma Ltd.';
const ORNEK = 'Örnek Tekstil A.Ş.';

const bakiyeOf = (b: Bakiye, fason: string, iplik: string) =>
  Number(
    b.items.find((i) => i.subcontractorTitle === fason && i.yarnType === iplik)?.balanceKg ?? 0,
  );
const stokOf = (s: Stok, iplik: string) =>
  Number(s.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0);
const anlik = async (r: Robot) => ({
  bakiye: await r.api<Bakiye>('/api/v1/balances'),
  stok: await r.api<Stok>('/api/v1/central-stock'),
  ozet: await r.api<Ozet>('/api/v1/reports/dashboard'),
});

async function acilisBakiyesi(mudur: Robot, fason: string, iplik: string, kg: number) {
  await mudur.sayfa.goto('/panel/raporlar');
  await mudur.sayfa.getByRole('tab', { name: 'Bakiye matrisi', exact: true }).click();
  await mudur.sayfa.getByLabel(/Satırlar/).fill(`${fason};${iplik};${String(kg)}`);
  await mudur.sayfa.getByRole('button', { name: /satırı yükle/ }).click();
  await expect(mudur.sayfa.getByText(/satır yüklendi/).first()).toBeVisible();
}

test('Sayısal doğrulama: başka fasoncudan ve kapıdan aktarma sevkiyatı bakiye ve stoğu doğru etkiler', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT) /);
  const iplik = damga();
  const y = { depo, mudur };

  await mudur.adim('açılış bakiyesi: Örnek Tekstil elinde 200 kg iplik tutar', async () => {
    const once = await anlik(mudur);
    await acilisBakiyesi(mudur, ORNEK, iplik, 200);
    const sonra = await anlik(mudur);
    mudur.kontrol(
      'Örnek Tekstil bakiyesi tam 200 kg arttı',
      kiloEsit(bakiyeOf(sonra.bakiye, ORNEK, iplik) - bakiyeOf(once.bakiye, ORNEK, iplik), 200),
      String(bakiyeOf(sonra.bakiye, ORNEK, iplik)),
    );
  });

  await mudur.adim(
    'başka fasoncudan (Örnek → Başka Dokuma) 100 kg: gönderen −100, alıcı +100, merkez stok değişmez',
    async () => {
      const once = await anlik(mudur);
      await tamamlanmisSevkiyat(y, iplik, '100', '3', { tur: 'fason', gonderen: 'Örnek Tekstil' });
      const sonra = await anlik(mudur);
      mudur.kontrol(
        'gönderen fasoncunun bakiyesi 100 kg azaldı',
        kiloEsit(bakiyeOf(once.bakiye, ORNEK, iplik) - bakiyeOf(sonra.bakiye, ORNEK, iplik), 100),
        `${bakiyeOf(once.bakiye, ORNEK, iplik)} → ${bakiyeOf(sonra.bakiye, ORNEK, iplik)}`,
      );
      mudur.kontrol(
        'alıcı fasoncunun bakiyesi 100 kg arttı',
        kiloEsit(bakiyeOf(sonra.bakiye, BASKA, iplik) - bakiyeOf(once.bakiye, BASKA, iplik), 100),
        `${bakiyeOf(sonra.bakiye, BASKA, iplik)}`,
      );
      mudur.kontrol(
        'merkez stok bu iplikte değişmedi',
        kiloEsit(stokOf(sonra.stok, iplik), stokOf(once.stok, iplik)),
        `${stokOf(once.stok, iplik)} → ${stokOf(sonra.stok, iplik)}`,
      );
    },
  );

  await mudur.adim(
    'kapıdan aktarma 100 kg: alıcı +100; merkez stok net değişmez (giriş ve çıkış)',
    async () => {
      const once = await anlik(mudur);
      await tamamlanmisSevkiyat(y, `${iplik}-K`, '100', '3', {
        tur: 'kapi',
        tedarikci: 'ROBOT Tedarikçi',
      });
      const sonra = await anlik(mudur);
      mudur.kontrol(
        'alıcı fasoncunun bakiyesi 100 kg arttı',
        kiloEsit(
          bakiyeOf(sonra.bakiye, BASKA, `${iplik}-K`) - bakiyeOf(once.bakiye, BASKA, `${iplik}-K`),
          100,
        ),
        `${bakiyeOf(sonra.bakiye, BASKA, `${iplik}-K`)}`,
      );
      mudur.kontrol(
        'merkez stokta bu iplik net 0 kg',
        kiloEsit(stokOf(sonra.stok, `${iplik}-K`), 0),
        `${stokOf(sonra.stok, `${iplik}-K`)}`,
      );
    },
  );
});

test('Sayısal doğrulama: taslak iptali stoğu, bakiyeyi ve tamamlanan sayısını değiştirmez', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT) /);
  const iplik = damga();
  const once = await anlik(mudur);
  const id = await taslakAc(mudur, iplik, '100');
  await cuvalEkle(depo, id, 2, '50');
  await mudur.adim('iptal et', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('button', { name: 'İptal et' }).click();
    await mudur.sayfa.getByLabel(/İptal gerekçesi/).fill('Robot sayısal iptal');
    await mudur.sayfa.getByRole('button', { name: 'Sevkiyatı iptal et' }).click();
    await expect(mudur.sayfa.locator('main')).toContainText(/iptal/i);
  });
  await mudur.adim('doğrula: sayılar yalnızca "iptal" sayacında değişir', async () => {
    const sonra = await anlik(mudur);
    mudur.kontrol(
      'toplam bakiye değişmedi',
      kiloEsit(Number(sonra.bakiye.totalKg), Number(once.bakiye.totalKg)),
      `${once.bakiye.totalKg} → ${sonra.bakiye.totalKg}`,
    );
    mudur.kontrol('merkez stokta bu iplik yok', stokOf(sonra.stok, iplik) === 0);
    const a = once.ozet.shipments;
    const b = sonra.ozet.shipments;
    mudur.kontrol(
      'iptal sayacı +1, tamamlanan aynı',
      (b.cancelled ?? 0) === (a.cancelled ?? 0) + 1 && (b.completed ?? 0) === (a.completed ?? 0),
      JSON.stringify({ a, b }),
    );
    mudur.kontrol('toplam sevkiyat +1', b.total === (a.total ?? 0) + 1);
  });
});

test('Sayısal doğrulama: iplik dönüşü, fire oranı ve itiraz fire tabanını ve bakiyeyi kural formülüyle değiştirir', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  for (const r of [depo, mudur, kalite]) r.beklenen(/4\d\d (POST|PUT) /);
  const iplik = damga();
  const id = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100', '3');
  const fire = () => mudur.api<Fire>(`/api/v1/shipments/${id}/fire-settlement`);
  const kontrolEt = async (ad: string, girdi: Parameters<typeof fireHesapla>[0]) => {
    const f = await fire();
    const h = fireHesapla(girdi);
    mudur.kontrol(
      `${ad}: taban ${h.taban}, beklenen ${h.beklenen}, hatalı ${h.hatali}`,
      kiloEsit(Number(f.baseKg), h.taban) &&
        kiloEsit(Number(f.expectedFabricKg), h.beklenen) &&
        kiloEsit(Number(f.faultyKg), h.hatali),
      JSON.stringify(f),
    );
  };

  await mudur.adim('iplik dönüşü 10 kg: bakiye 90, fire tabanı 90', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('tab', { name: 'İplik dönüşü', exact: true }).click();
    await mudur.sayfa
      .getByLabel(/İplik cinsi/)
      .first()
      .fill(iplik);
    await mudur.sayfa.getByLabel(/Net kilo/).fill('10');
    await mudur.sayfa.getByRole('button', { name: 'Hareketi kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    mudur.kontrol(
      'Başka Dokuma bakiyesi 90 kg',
      kiloEsit(bakiyeOf((await anlik(mudur)).bakiye, BASKA, iplik), 90),
    );
    await kontrolEt('dönüş sonrası', {
      gonderilen: 100,
      donen: 10,
      itiraz: 0,
      kumas: 0,
      oran: 0.03,
    });
  });

  await mudur.adim('fire oranı %5: beklenen taban × 0,95', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('tab', { name: 'Fire dosyası', exact: true }).click();
    await mudur.sayfa.getByLabel(/Fire oranı \(%\)/).fill('5');
    await mudur.sayfa.getByLabel(/Değişiklik gerekçesi/).fill('Robot: sayısal doğrulama');
    await mudur.sayfa.getByRole('button', { name: 'Oranı kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    await kontrolEt('oran %5 sonrası', {
      gonderilen: 100,
      donen: 10,
      itiraz: 0,
      kumas: 0,
      oran: 0.05,
    });
  });

  await mudur.adim('kumaş 80 kg: hatalı kilo formülle aynı', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }).click();
    await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
    await mudur.sayfa.getByLabel(/Kumaş net kilosu/).fill('80');
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-K`);
    await mudur.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    await kontrolEt('kumaş sonrası', {
      gonderilen: 100,
      donen: 10,
      itiraz: 0,
      kumas: 80,
      oran: 0.05,
    });
  });

  await kalite.adim(
    'haklı itiraz 10 kg: bakiye −10 (0), fire tabanı ve hatalı kilo itirazı hesaba katar',
    async () => {
      await kalite.sayfa.goto(`/panel/sevkiyat/${id}`);
      await kalite.sayfa.getByRole('tab', { name: 'Kalite itirazı', exact: true }).click();
      await kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' }).click();
      await kalite.sayfa.getByLabel(/İtiraz edilen kilo/).fill('10');
      await kalite.sayfa.getByLabel(/Sorunu anlatın/).fill('Robot: sayısal itiraz');
      await kalite.sayfa
        .locator('input[type=file]')
        .last()
        .setInputFiles({
          name: 'i.jpg',
          mimeType: 'image/jpeg',
          buffer: await resim(kalite.sayfa, 13),
        });
      await kalite.sayfa.getByText('1 fotoğraf eklendi.').waitFor();
      await kalite.sayfa.getByRole('button', { name: 'İtirazı gönder' }).click();
      await expect(kalite.sayfa.locator('main')).toContainText(/UY-SV-/);
      const no = (await kalite.api<{ shipmentNo: string }>(`/api/v1/shipments/${id}`)).shipmentNo;
      await kalite.sayfa.goto('/panel/uyusmazliklar');
      await kalite.sayfa.getByRole('link', { name: new RegExp(no) }).click();
      await kalite.sayfa.getByRole('button', { name: 'Bana ata' }).click();
      await sec(kalite.sayfa, /^Karar/, 'Talep haklı');
      await kalite.sayfa.getByLabel(/^Not/).first().fill('Robot: haklı');
      await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
      await kalite.sayfa.waitForTimeout(2000);
      mudur.kontrol(
        'Başka Dokuma bakiyesi 0 kg (100 − 10 dönüş − 80 kumaş − 10 itiraz)',
        kiloEsit(bakiyeOf((await anlik(mudur)).bakiye, BASKA, iplik), 0),
        String(bakiyeOf((await anlik(mudur)).bakiye, BASKA, iplik)),
      );
      await kontrolEt('itiraz sonrası', {
        gonderilen: 100,
        donen: 10,
        itiraz: 10,
        kumas: 80,
        oran: 0.05,
      });
      const f = await fire();
      mudur.kontrol('fire dosyasında itiraz kilosu 10', kiloEsit(Number(f.claimKg), 10), f.claimKg);
    },
  );
});
