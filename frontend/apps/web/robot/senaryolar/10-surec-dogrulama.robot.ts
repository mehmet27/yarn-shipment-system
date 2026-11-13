import { tamamlanmisSevkiyat } from '../support/akis';
import { expect, test } from '../support/robot';
import { damga } from '../support/yardimci';

type Dashboard = {
  shipments: Record<string, number>;
  quantity: { dispatchNetKg: string; deliveryNetKg: string };
};
type Bakiye = {
  items: { subcontractorTitle: string; yarnType: string; balanceKg: string; inTransitKg: string }[];
  totalKg: string;
};
type Stok = {
  rows: { yarnType: string; stockKg: string }[];
  totalKg: string;
  producedFabricKg: string;
};
type Fire = {
  sentKg: string;
  fabricKg: string;
  baseKg: string;
  expectedFabricKg: string;
  faultyKg: string;
  actualFirePercent: string;
  fireRate: string;
  status: string;
};
type DenetimSayfasi = { page: { totalElements: number } };
type Liste = { items: { shipmentId: string; shipmentNo: string; status: string }[] };

const kg = (n: number) =>
  `${new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 3 }).format(n)} kg`;
const yakin = (a: number, b: number) => Math.abs(a - b) < 0.002;
/** Bu süreçte denetim kaydına düşmesi gereken, daha önce yazılmayan işlemler (olay türü → ekrandaki ad). */
const DENETIM_ISLEMLERI = {
  'com.yarnshipment.stock.yarn-receipt-recorded.v1': 'Merkez stok girişi yapıldı',
  'com.yarnshipment.stock.central-production-recorded.v1': 'Merkezde kumaş üretildi',
  'com.yarnshipment.shipment.fabric-received.v1': 'Kumaş girişi yapıldı',
} as const;

/**
 * Bilinen sayılarla süreci baştan sona yürütür ve her adımın sonucunu hem API'den hem ekrandan doğrular:
 * stok, bakiye, özet sayıları, fire hesabı, rapor satırları, ambar fişi ve denetim kaydı.
 */
test('Süreç doğrulama: 200 kg iplik → 50 kg kumaş → 100 kg sevkiyat → 90 kg kumaş; tüm sayılar tutmalı', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const kalite = await giris('kalite');
  const mudur = await giris('mudur');
  const admin = await giris('admin');
  for (const r of [depo, kalite, mudur, admin]) r.beklenen(/4\d\d (POST|PUT) /);
  const iplik = damga();
  const satirBul = (b: Bakiye) =>
    b.items.find((i) => i.yarnType === iplik && i.subcontractorTitle === 'Başka Dokuma Ltd.');

  const denetimSayisi = async (islem: string) =>
    (
      await admin.api<DenetimSayfasi>(
        `/api/v1/audit-log?action=${encodeURIComponent(islem)}&size=1`,
      )
    ).page.totalElements;
  const denetimOnce: Record<string, number> = {};
  for (const islem of Object.keys(DENETIM_ISLEMLERI))
    denetimOnce[islem] = await denetimSayisi(islem);

  const once = {
    ozet: await mudur.api<Dashboard>('/api/v1/reports/dashboard'),
    bakiye: await mudur.api<Bakiye>('/api/v1/balances'),
    stok: await depo.api<Stok>('/api/v1/central-stock'),
  };

  await depo.adim('doğrula: iplik girişi stoğu tam 200 kg artırır', async () => {
    await depo.sayfa.goto('/panel/merkez-stok');
    await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await depo.sayfa
      .getByLabel(/İplik cinsi/)
      .first()
      .fill(iplik);
    await depo.sayfa.getByLabel(/Gelen net kg/).fill('200');
    await depo.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await depo.sayfa.getByRole('tab', { name: 'Eldeki iplik' }).click();
    await expect(depo.sayfa.locator('main')).toContainText(`${iplik}`);
    const stok = await depo.api<Stok>('/api/v1/central-stock');
    depo.kontrol(
      'API: iplik stoğu 200 kg',
      yakin(Number(stok.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0), 200),
    );
    depo.kontrol(
      'API: toplam stok 200 kg arttı',
      yakin(Number(stok.totalKg) - Number(once.stok.totalKg), 200),
      `${stok.totalKg} − ${once.stok.totalKg}`,
    );
    depo.kontrol(
      'Ekran: iplik satırı "200 kg" göstermeli',
      (await depo.sayfa.innerText('main')).includes(kg(200)),
    );
  });

  await kalite.adim(
    'doğrula: 50 kg kumaş (fire %3) iplikten 50/0,97 = 51,546 kg düşer',
    async () => {
      await kalite.sayfa.goto('/panel/merkez-stok');
      await kalite.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
      await kalite.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
      await kalite.sayfa.getByLabel(/İplik cinsi 1/).fill(iplik);
      await kalite.sayfa.getByLabel(/Kumaş kg 1/).fill('50');
      await kalite.sayfa.getByLabel(/Fire \(%\)/).fill('3');
      await kalite.sayfa.getByRole('button', { name: 'Kumaşı kaydet' }).click();
      await expect(kalite.sayfa.getByLabel(/Kumaş cinsi/)).toHaveValue('');
      const stok = await kalite.api<Stok>('/api/v1/central-stock');
      const beklenen = 200 - 50 / 0.97;
      kalite.kontrol(
        'iplik stoğu 148,454 kg olmalı',
        yakin(Number(stok.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0), beklenen),
        String(stok.rows.find((r) => r.yarnType === iplik)?.stockKg),
      );
      kalite.kontrol(
        'üretilen kumaş toplamı 50 kg arttı',
        yakin(Number(stok.producedFabricKg) - Number(once.stok.producedFabricKg), 50),
        `${stok.producedFabricKg}`,
      );
    },
  );

  let id = '';
  await mudur.adim('süreç: 100 kg sevkiyatı tamamla (depo ve müdür ekranlarıyla)', async () => {
    id = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100', '3');
  });

  await mudur.adim("doğrula: sevkiyat verileri ekranda ve API'de aynı", async () => {
    const detay = await mudur.api<{
      dispatchWeighing: { grossKg: string; tareKg: string; netKg: string; bagCount: number };
      status: string;
      fireRate: string;
    }>(`/api/v1/shipments/${id}`);
    mudur.kontrol('durum COMPLETED', detay.status === 'COMPLETED', detay.status);
    mudur.kontrol(
      'tartı: brüt 103,5 − dara 3,5 = net 100, 2 çuval',
      Number(detay.dispatchWeighing.grossKg) === 103.5 &&
        Number(detay.dispatchWeighing.tareKg) === 3.5 &&
        Number(detay.dispatchWeighing.netKg) === 100 &&
        detay.dispatchWeighing.bagCount === 2,
      JSON.stringify(detay.dispatchWeighing),
    );
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await mudur.sayfa.getByRole('tab', { name: 'Kilolar', exact: true }).click();
    await mudur.sayfa.waitForTimeout(600);
    const metin = await mudur.sayfa.innerText('main');
    mudur.kontrol(
      'Ekran: dosya TAMAMLANDI; Kilolar sekmesi net 100 kg, 2 çuval gösterir',
      /tamamlandı/i.test(metin) && metin.includes('100 kg') && /Çuval\s*2/.test(metin),
    );
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}/ambar-cikis-fisi`);
    await mudur.sayfa.waitForLoadState('networkidle');
    const fis = (await mudur.sayfa.locator('body').innerText()).replace(/\s+/g, ' ');
    mudur.kontrol(
      'Ambar fişi: 2 çuval × 50 kg, genel toplam 100 kg',
      fis.includes('50 kg') && /Genel Toplam:\s*2 Ad\.\s*~\s*100 kg/.test(fis),
      fis.slice(-160),
    );
  });

  await mudur.adim('doğrula: stok, bakiye ve özet sayıları bu sevkiyatla tam değişir', async () => {
    const stok = await mudur.api<Stok>('/api/v1/central-stock');
    mudur.kontrol(
      'merkez stoktan 100 kg daha düşer (148,454 → 48,454)',
      yakin(
        Number(stok.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0),
        200 - 50 / 0.97 - 100,
      ),
      String(stok.rows.find((r) => r.yarnType === iplik)?.stockKg),
    );
    const bakiye = await mudur.api<Bakiye>('/api/v1/balances');
    mudur.kontrol(
      'Başka Dokuma bakiyesi bu iplikten tam 100 kg',
      yakin(Number(satirBul(bakiye)?.balanceKg ?? 0), 100),
      String(satirBul(bakiye)?.balanceKg),
    );
    mudur.kontrol(
      'toplam bakiye 100 kg arttı',
      yakin(Number(bakiye.totalKg) - Number(once.bakiye.totalKg), 100),
      `${bakiye.totalKg} − ${once.bakiye.totalKg}`,
    );
    const ozet = await mudur.api<Dashboard>('/api/v1/reports/dashboard');
    mudur.kontrol(
      'özet: toplam sevkiyat +1, tamamlanan +1',
      ozet.shipments.total === (once.ozet.shipments.total ?? 0) + 1 &&
        ozet.shipments.completed === (once.ozet.shipments.completed ?? 0) + 1,
      JSON.stringify(ozet.shipments),
    );
    mudur.kontrol(
      'özet: çıkış net ve teslim net +100 kg',
      yakin(Number(ozet.quantity.dispatchNetKg) - Number(once.ozet.quantity.dispatchNetKg), 100) &&
        yakin(Number(ozet.quantity.deliveryNetKg) - Number(once.ozet.quantity.deliveryNetKg), 100),
    );
    const toplam = ozet.shipments;
    mudur.kontrol(
      'özet kendi içinde tutar',
      toplam.total ===
        (toplam.inProgress ?? 0) +
          (toplam.inReview ?? 0) +
          (toplam.completed ?? 0) +
          (toplam.cancelled ?? 0),
    );
    await mudur.sayfa.goto('/panel/bakiye');
    await mudur.sayfa.waitForLoadState('networkidle');
    const bakiyeSayfa = await mudur.sayfa.innerText('main');
    mudur.kontrol(
      'Fasoncu bakiyesi ekranı iplik satırını ve 100 kg değerini gösterir',
      bakiyeSayfa.includes(iplik) && bakiyeSayfa.includes('100 kg'),
    );
  });

  await mudur.adim(
    'doğrula: 90 kg kumaş → fire hesabı (beklenen 97, hatalı 7, fire %10)',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
      await mudur.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }).click();
      await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
      await mudur.sayfa.getByLabel(/Kumaş net kilosu/).fill('90');
      await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-K`);
      await mudur.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
      await mudur.sayfa.waitForTimeout(1500);
      const fire = await mudur.api<Fire>(`/api/v1/shipments/${id}/fire-settlement`);
      mudur.kontrol(
        'API: G=100, K=90, T=100, B=97, hatalı=7',
        Number(fire.sentKg) === 100 &&
          Number(fire.fabricKg) === 90 &&
          Number(fire.baseKg) === 100 &&
          yakin(Number(fire.expectedFabricKg), 97) &&
          yakin(Number(fire.faultyKg), 7),
        JSON.stringify(fire),
      );
      mudur.kontrol(
        'API: gerçekleşen fire %10, durum faturalanacak',
        yakin(Number(fire.actualFirePercent), 10) && fire.status === 'TO_INVOICE',
        `${fire.actualFirePercent} ${fire.status}`,
      );
      await mudur.sayfa.getByRole('tab', { name: 'Fire dosyası', exact: true }).click();
      await mudur.sayfa.waitForTimeout(600);
      const metin = (await mudur.sayfa.innerText('main')).replace(/\s+/g, ' ');
      mudur.kontrol(
        'Ekran: Fire dosyası aynı sayıları gösterir',
        metin.includes('97 kg') &&
          metin.includes('7 kg') &&
          metin.includes('90 kg') &&
          /%10/.test(metin),
        metin.slice(metin.indexOf('Fire dosyası'), metin.indexOf('Fire dosyası') + 260),
      );
      await mudur.sayfa.goto('/panel/faturalanacaklar');
      await mudur.sayfa.waitForLoadState('networkidle');
      await mudur.sayfa.waitForTimeout(800);
      mudur.kontrol(
        'Faturalanacaklar listesinde bu sevkiyat 7 kg hatalı kiloyla görünür',
        (await mudur.sayfa.innerText('main')).includes('7 kg'),
      );
    },
  );

  await mudur.adim('doğrula: raporlar bu sevkiyatı aynı sayılarla gösterir', async () => {
    const liste = await mudur.api<Liste>('/api/v1/shipments?size=100');
    const no = liste.items.find((s) => s.shipmentId === id)?.shipmentNo ?? '';
    await mudur.sayfa.goto('/panel/raporlar');
    await mudur.sayfa.getByRole('tab', { name: 'Sevkiyat dökümü', exact: true }).click();
    await mudur.sayfa.waitForTimeout(800);
    const satir = mudur.sayfa.locator('tr', { hasText: no });
    const satirMetin = (
      await satir
        .first()
        .innerText()
        .catch(() => '')
    ).replace(/\s+/g, ' ');
    mudur.kontrol(
      'Sevkiyat dökümü: tamamlandı, çıkış ve teslim net 100 kg',
      /Tamamlandı/.test(satirMetin) && (satirMetin.match(/100 kg/g) ?? []).length >= 2,
      satirMetin,
    );
    await mudur.sayfa.getByRole('tab', { name: 'Fasoncu bakiye', exact: true }).click();
    await mudur.sayfa.waitForTimeout(800);
    mudur.kontrol(
      'Fasoncu bakiye raporu Başka Dokuma satırını gösterir',
      (await mudur.sayfa.innerText('main')).includes('Başka Dokuma Ltd.'),
    );
  });

  await admin.adim('doğrula: denetim kaydı bu işlemleri yapan kişilerle yazılmış', async () => {
    await admin.sayfa.goto('/panel/denetim');
    await admin.sayfa.waitForLoadState('networkidle');
    const metin = await admin.sayfa.innerText('main');
    admin.kontrol(
      'Denetim kaydında "Sevkiyat tamamlandı" ve müdür adı görünür',
      /tamamlandı/i.test(metin) && metin.includes('Melek Müdür'),
      metin.slice(0, 200),
    );
    admin.kontrol(
      'Denetim kaydında sevkiyat oluşturma ve kanıt yükleme görünür',
      /Sevkiyat oluşturuldu/.test(metin) && /Kanıt yüklendi/.test(metin),
    );
    for (const [islem, ad] of Object.entries(DENETIM_ISLEMLERI)) {
      const sonra = await denetimSayisi(islem);
      admin.kontrol(
        `Denetim kaydına "${ad}" işlemi yazıldı`,
        sonra === (denetimOnce[islem] ?? 0) + 1,
        `${denetimOnce[islem]} → ${sonra}`,
      );
    }
    await admin.sayfa.goto('/panel/denetim?varlik=CentralStock');
    await admin.sayfa.waitForLoadState('networkidle');
    const stokMetni = await admin.sayfa.innerText('main');
    admin.kontrol(
      'Denetim sayfası "Merkez stok" kayıtlarını ve Türkçe işlem adlarını gösterir',
      stokMetni.includes('Merkezde kumaş üretildi') &&
        stokMetni.includes('Merkez stok girişi yapıldı'),
      stokMetni.slice(0, 200),
    );
  });
});
