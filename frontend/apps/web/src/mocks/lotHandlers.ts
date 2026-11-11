import type { LotView, Permission } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { matchesQuery } from './masterData';
import type { MockLotInput } from './lots';
import {
  findMockLot,
  isDuplicateLotNo,
  listMockLots,
  newMockLotId,
  saveMockLot,
  toMockLot,
} from './lots';

const PATH = '/api/v1/master-data/lots';
const DEFAULT_PAGE_SIZE = 20;

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });
const denied = () => problem(403, 'FORBIDDEN', 'Yetkiniz yok');
const notFound = () => problem(404, 'MASTER_DATA_NOT_FOUND', 'Kayıt bulunamadı');
const locked = () => problem(409, 'LOT_QUALITY_LOCKED', 'Partinin kalitesi kilitli');
const invalid = () => problem(400, 'INVALID_MASTER_DATA', 'Geçersiz kayıt');
const duplicate = () => problem(409, 'DUPLICATE_CODE', 'Kod zaten kayıtlı');

const FIELDS = ['lotNo', 'productId', 'qualityGradeId', 'productionDate'] as const;

function readInput(json: unknown, siteId: string | undefined): MockLotInput | undefined {
  if (typeof json !== 'object' || json === null) return undefined;
  const body = json as Record<string, unknown>;
  const site = siteId ?? body.siteId;
  if (typeof site !== 'string' || FIELDS.some((field) => typeof body[field] !== 'string')) {
    return undefined;
  }
  const text = (field: (typeof FIELDS)[number]) => String(body[field]);
  const supplier = body.yarnSupplierId;
  return {
    siteId: site,
    lotNo: text('lotNo'),
    productId: text('productId'),
    qualityGradeId: text('qualityGradeId'),
    ...(typeof supplier === 'string' && supplier !== '' ? { yarnSupplierId: supplier } : {}),
    productionDate: text('productionDate'),
  };
}

const listHandler = http.get(PATH, ({ request }) => {
  const params = new URL(request.url).searchParams;
  const isIncludingInactive = params.get('status') === 'ALL';
  const page = Number(params.get('page') ?? '0');
  const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
  const matching = listMockLots().filter(
    (lot) =>
      (isIncludingInactive || lot.active) &&
      matchesQuery(`${lot.lotNo} ${lot.productCode} ${lot.productName}`, params.get('q')),
  );
  return HttpResponse.json({
    items: matching.slice(page * size, (page + 1) * size),
    page: {
      number: page,
      size,
      totalElements: matching.length,
      totalPages: Math.ceil(matching.length / size),
    },
  });
});

async function create(request: Request) {
  const input = readInput(await request.json(), undefined);
  const lot = input && toMockLot(newMockLotId(), input);
  if (lot === undefined) return invalid();
  if (isDuplicateLotNo(lot.siteId, lot.lotNo)) return duplicate();
  saveMockLot(lot);
  return HttpResponse.json(lot, { status: 201 });
}

async function edit(request: Request, current: LotView) {
  const input = readInput(await request.json(), current.siteId);
  const lot = input && toMockLot(current.id, input);
  if (lot === undefined) return invalid();
  if (isDuplicateLotNo(lot.siteId, lot.lotNo, current.id)) return duplicate();
  const edited = { ...lot, active: current.active };
  saveMockLot(edited);
  return HttpResponse.json(edited);
}

function writeHandlers() {
  return [
    http.post(PATH, ({ request }) => create(request)),
    http.put(`${PATH}/:id`, ({ params, request }) => {
      const current = findMockLot(String(params.id));
      if (current === undefined) return notFound();
      return current.qualityLockedAt === undefined ? edit(request, current) : locked();
    }),
    http.post(`${PATH}/:id/quality-lock`, ({ params }) => {
      const current = findMockLot(String(params.id));
      if (current === undefined) return notFound();
      if (current.qualityLockedAt !== undefined) return locked();
      const lockedLot = { ...current, qualityLockedAt: new Date().toISOString() };
      saveMockLot(lockedLot);
      return HttpResponse.json(lockedLot);
    }),
    ...(['deactivate', 'activate'] as const).map((action) =>
      http.post(`${PATH}/:id/${action}`, ({ params }) => {
        const current = findMockLot(String(params.id));
        if (current === undefined) return notFound();
        saveMockLot({ ...current, active: action === 'activate' });
        return new HttpResponse(null, { status: 204 });
      }),
    ),
  ];
}

/** Parti uçları; yazma ve pasifleri listeleme yalnızca LOT_QUALITY_LOCK izniyle (403 aksi halde). */
export function createLotHandlers(permissions: readonly Permission[]) {
  if (!permissions.includes('LOT_QUALITY_LOCK')) {
    return [
      http.all(`${PATH}*`, ({ request }) => {
        const isActiveList = request.method === 'GET' && !request.url.includes('status=ALL');
        return isActiveList ? undefined : denied();
      }),
      listHandler,
    ];
  }
  return [listHandler, ...writeHandlers()];
}
