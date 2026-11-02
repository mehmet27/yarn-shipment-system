import type { LabeledBagView, LockedLotView, PackageType, Permission } from '@iplik/api-client';
import { BAGS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';

const MAX_COUNT = 200;
const KEY_ALPHABET = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const KEY_GROUP_LENGTH = 4;
/** Sahte PDF; tarayıcıda boş sayfa açılır, testte yalnızca türü önemlidir. */
const MOCK_PDF = '%PDF-1.4\n%%EOF\n';

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

type MockLot = Omit<LockedLotView, 'bagCount'>;

const LOTS: readonly MockLot[] = [
  {
    lotId: 'lot-0001',
    siteId: 'site-a',
    lotNo: 'P-2026-0001',
    qualityGradeCode: 'A',
    lockedAt: '2026-10-30T08:00:00Z',
  },
  {
    lotId: 'lot-0002',
    siteId: 'site-a',
    lotNo: 'P-2026-0002',
    qualityGradeCode: 'B',
    lockedAt: '2026-10-29T08:00:00Z',
  },
];

function randomGroup(): string {
  return Array.from(
    { length: KEY_GROUP_LENGTH },
    () => KEY_ALPHABET[Math.floor(Math.random() * KEY_ALPHABET.length)],
  ).join('');
}

function seedBags(): Map<string, LabeledBagView[]> {
  const printed: LabeledBagView = {
    bag: {
      id: 'bag-0001',
      uniqueKey: 'BAG-7K3M-Q9XD',
      packageType: 'BAG',
      lotNo: 'P-2026-0001',
      qualityGradeCode: 'A',
      status: 'CREATED',
    },
    printCount: 1,
    lastPrintedAt: '2026-10-30T09:00:00Z',
  };
  return new Map([[LOTS[0]?.lotId ?? '', [printed]]]);
}

type CreateBody = { lotId: string; packageType: PackageType; count: number };
type PrintBody = { uniqueKeys: string[]; reprintReason?: string };

/** bag-service'in sahte karşılığı: kilitli partiler, çuval üretimi ve etiket baskısı (her çağrıda temiz). */
export function createBagHandlers(permissions: readonly Permission[]) {
  const bagsByLot = seedBags();
  const canCreate = permissions.includes('BAG_CREATE');
  const forbidden = () => problem(403, 'ACCESS_DENIED', 'Yetki yok');
  const allBags = () => [...bagsByLot.values()].flat();

  return [
    http.get(`${BAGS_PATH}/lots`, () => {
      if (!canCreate) return forbidden();
      const lots = LOTS.map((lot) => ({ ...lot, bagCount: bagsByLot.get(lot.lotId)?.length ?? 0 }));
      return HttpResponse.json({ lots });
    }),
    http.get(`${BAGS_PATH}/lots/:lotId/bags`, ({ params }) => {
      if (!canCreate) return forbidden();
      return HttpResponse.json({ bags: bagsByLot.get(String(params.lotId)) ?? [] });
    }),
    http.post(BAGS_PATH, async ({ request }) => {
      if (!canCreate) return forbidden();
      const body = (await request.json()) as CreateBody;
      const lot = LOTS.find((candidate) => candidate.lotId === body.lotId);
      if (lot === undefined) {
        return problem(409, 'LOT_NOT_LOCKED', 'Partinin kalitesi kilitli değil');
      }
      if (!Number.isInteger(body.count) || body.count < 1 || body.count > MAX_COUNT) {
        return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
      }
      const created = Array.from({ length: body.count }, () => ({
        id: crypto.randomUUID(),
        uniqueKey: `BAG-${randomGroup()}-${randomGroup()}`,
        packageType: body.packageType,
        lotNo: lot.lotNo,
        qualityGradeCode: lot.qualityGradeCode,
        status: 'CREATED' as const,
      }));
      const existing = bagsByLot.get(lot.lotId) ?? [];
      bagsByLot.set(lot.lotId, [...created.map((bag) => ({ bag, printCount: 0 })), ...existing]);
      return HttpResponse.json({ bags: created }, { status: 201 });
    }),
    http.post(`${BAGS_PATH}/labels`, async ({ request }) => {
      if (!canCreate) return forbidden();
      const body = (await request.json()) as PrintBody;
      const selected = allBags().filter((item) => body.uniqueKeys.includes(item.bag.uniqueKey));
      const hasReason = (body.reprintReason ?? '').trim() !== '';
      if (selected.some((item) => item.printCount > 0) && !hasReason) {
        return problem(422, 'LABEL_REPRINT_REASON_REQUIRED', 'Yeniden baskı gerekçesi gerekli');
      }
      const now = new Date().toISOString();
      selected.forEach((item) => {
        item.printCount += 1;
        item.lastPrintedAt = now;
      });
      return new HttpResponse(MOCK_PDF, { headers: { 'Content-Type': 'application/pdf' } });
    }),
  ];
}
