import { KAPSANAN } from '../support/kapsam';
import { sayfayiGez } from '../support/gezgin';
import type { Kapsam } from '../support/gezgin';
import { rolTanimli, test } from '../support/robot';

/** Rol → gezilecek sabit rotalar (yetki matrisiyle aynı; 01-gezinti). Sevkiyat dosyaları duruma göre eklenir. */
const ROTALAR: Record<string, string[]> = {
  admin: [
    '/panel',
    '/panel/sevkiyat',
    '/panel/sevkiyat/yeni',
    '/panel/uyusmazliklar',
    '/panel/tanimlar',
    '/panel/partiler',
    '/panel/raporlar',
    '/panel/bakiye',
    '/panel/merkez-stok',
    '/panel/kullanicilar',
    '/panel/denetim',
    '/panel/gorunum',
  ],
  depo: ['/panel', '/panel/sevkiyat', '/panel/merkez-stok'],
  kalite: [
    '/panel',
    '/panel/sevkiyat',
    '/panel/uyusmazliklar',
    '/panel/partiler',
    '/panel/raporlar',
    '/panel/bakiye',
    '/panel/merkez-stok',
    '/panel/denetim',
  ],
  mudur: [
    '/panel',
    '/panel/sevkiyat',
    '/panel/sevkiyat/yeni',
    '/panel/uyusmazliklar',
    '/panel/raporlar',
    '/panel/bakiye',
    '/panel/faturalanacaklar',
    '/panel/merkez-stok',
    '/panel/denetim',
  ],
  'genel-mudur': ['/panel', '/panel/raporlar', '/panel/bakiye'],
};

type Liste = { items: { shipmentId: string; status: string }[] };

const tumKapsam: (Kapsam & { rol: string })[] = [];

for (const [rol, rotalar] of Object.entries(ROTALAR)) {
  test(`Tüm ekranlar: ${rol} her sekme, filtre ve güvenli düğmeye tıklar`, async ({ giris }) => {
    test.skip(!rolTanimli(rol), `${rol} için kullanıcı/şifre tanımlı değil`);
    const robot = await giris(rol);
    const yollar = [...rotalar];
    if (rol !== 'genel-mudur') {
      const liste = await robot.api<Liste>('/api/v1/shipments?size=100');
      const durumlar = new Map<string, string>();
      for (const s of liste.items)
        if (!durumlar.has(s.status)) durumlar.set(s.status, s.shipmentId);
      for (const id of durumlar.values()) yollar.push(`/panel/sevkiyat/${id}`);
    }
    if (rol !== 'genel-mudur') {
      const uy = await robot
        .api<{ items: { id?: string; discrepancyId?: string }[] }>('/api/v1/discrepancies?size=1')
        .catch(() => undefined);
      const uyId = uy?.items[0]?.id ?? uy?.items[0]?.discrepancyId;
      if (uyId !== undefined && rol !== 'depo') yollar.push(`/panel/uyusmazliklar/${uyId}`);
      const tam = (await robot.api<Liste>('/api/v1/shipments?size=100')).items.find(
        (s) => s.status === 'COMPLETED',
      );
      if (tam !== undefined) yollar.push(`/panel/sevkiyat/${tam.shipmentId}/ambar-cikis-fisi`);
      const tas = (await robot.api<Liste>('/api/v1/shipments?size=100')).items.find(
        (s) => s.status === 'DRAFT',
      );
      if (tas !== undefined && rol === 'depo')
        yollar.push(`/panel/sevkiyat/${tas.shipmentId}/cikis`);
    }
    const kapsanan = new Set(Object.keys(KAPSANAN));
    for (const yol of yollar)
      tumKapsam.push(...(await sayfayiGez(robot, yol, kapsanan)).map((k) => ({ ...k, rol })));
    const acik = tumKapsam.filter((k) => k.rol === rol && k.durum === 'AÇIKTA');
    for (const k of acik)
      robot.kontrol(
        `Kapsam açığı: ${rol} ${k.rota} ${k.ad} — hiçbir senaryoda denenmiyor (support/kapsam.ts)`,
        false,
      );
  });
}
