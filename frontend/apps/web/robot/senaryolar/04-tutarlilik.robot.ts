import { test } from '../support/robot';

type Dashboard = {
  shipments: Record<string, number>;
  quantity: { dispatchNetKg: string; deliveryNetKg: string };
};
type Bakiye = { items: { balanceKg: string; inTransitKg: string }[]; totalKg: string };
type Liste = { items: { shipmentId: string; shipmentNo: string; status: string }[] };
type Fire = {
  fireRate: string;
  sentKg: string;
  fabricKg: string;
  returnedKg: string;
  baseKg: string;
  expectedFabricKg: string;
};
type Detay = { fireRate: string };

const topla = (degerler: string[]) => degerler.reduce((t, d) => t + Number(d), 0);
const yakin = (a: number, b: number) => Math.abs(a - b) < 0.0005;

test('Tutarlılık: özet, bakiye ve fire dosyası sayıları birbirini tutmalı', async ({ giris }) => {
  const mudur = await giris('mudur');
  mudur.beklenen(/404 GET \/api\/v1\/shipments\/[\w-]+\/fire-settlement/);

  await mudur.adim('özet sevkiyat sayıları kendi içinde tutar', async () => {
    const ozet = await mudur.api<Dashboard>('/api/v1/reports/dashboard');
    const s = ozet.shipments;
    mudur.kontrol(
      'toplam = süren + incelemede + tamamlanan + iptal',
      s.total === (s.inProgress ?? 0) + (s.inReview ?? 0) + (s.completed ?? 0) + (s.cancelled ?? 0),
      JSON.stringify(s),
    );
  });

  await mudur.adim('fasoncu bakiye toplamı satırların toplamına eşit', async () => {
    const bakiye = await mudur.api<Bakiye>('/api/v1/balances');
    mudur.kontrol(
      'totalKg = Σ balanceKg',
      yakin(Number(bakiye.totalKg), topla(bakiye.items.map((i) => i.balanceKg))),
      `${bakiye.totalKg} ≠ ${String(topla(bakiye.items.map((i) => i.balanceKg)))}`,
    );
    mudur.kontrol(
      'fasoncu bakiyesi eksiye düşmemeli',
      bakiye.items.every((i) => Number(i.balanceKg) >= 0),
      'eksi bakiye satırı var',
    );
  });

  await mudur.adim(
    'her tamamlanan sevkiyatın fire dosyası sevkiyat oranıyla ve iplik kilosuyla tutar',
    async () => {
      const liste = await mudur.api<Liste>('/api/v1/shipments?size=100');
      for (const sevkiyat of liste.items.filter((x) => x.status === 'COMPLETED')) {
        const detay = await mudur.api<Detay>(`/api/v1/shipments/${sevkiyat.shipmentId}`);
        const fire = await mudur
          .api<Fire>(`/api/v1/shipments/${sevkiyat.shipmentId}/fire-settlement`)
          .catch(() => undefined);
        if (fire === undefined) {
          mudur.kontrol(`${sevkiyat.shipmentNo}: tamamlanan sevkiyatın fire dosyası olmalı`, false);
          continue;
        }
        mudur.kontrol(
          `${sevkiyat.shipmentNo}: fire oranı sevkiyatla aynı`,
          Number(fire.fireRate) === Number(detay.fireRate),
          `${fire.fireRate} ≠ ${detay.fireRate}`,
        );
        mudur.kontrol(
          `${sevkiyat.shipmentNo}: kumaş ≤ gönderilen iplik`,
          Number(fire.fabricKg) <= Number(fire.sentKg),
          `${fire.fabricKg} > ${fire.sentKg}`,
        );
        mudur.kontrol(
          `${sevkiyat.shipmentNo}: fire tabanı = gönderilen − dönen`,
          yakin(Number(fire.baseKg), Number(fire.sentKg) - Number(fire.returnedKg)),
          `${fire.baseKg}`,
        );
        mudur.kontrol(
          `${sevkiyat.shipmentNo}: beklenen kumaş = taban × (1 − fire)`,
          yakin(Number(fire.expectedFabricKg), Number(fire.baseKg) * (1 - Number(fire.fireRate))),
          `${fire.expectedFabricKg}`,
        );
      }
    },
  );
});
