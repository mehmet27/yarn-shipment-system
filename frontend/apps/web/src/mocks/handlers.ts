import type {
  InternalShipmentView,
  RecipeLineInput,
  Role,
  WeightCheckView,
  Weighing,
  WeightDecisionKind,
} from '@iplik/api-client';
import { ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { todayIsoDate } from '@iplik/i18n';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, isKilogram, subtractKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import {
  assignMockBagWeights,
  updateMockShipmentDate,
  addMockShipment,
  completeMockShipment,
  decideMockWeight,
  findMockShipment,
  listMockShipments,
  recordMockDispatch,
  updateMockShipmentStatus,
} from './mockStore';
import { bagMismatchResponse } from './bagChecks';
import { createAuditHandlers } from './auditHandlers';
import { createBrandingHandlers } from './brandingHandlers';
import { createRolePermissionHandlers } from './rolePermissionHandlers';
import { createClaimHandlers } from './claimHandlers';
import { createDiscrepancyHandlers } from './discrepancyHandlers';
import { createEvidenceHandlers } from './evidenceHandlers';
import { createLoginAccountHandlers } from './loginAccountHandlers';
import { requestMockPassword } from './loginAccounts';
import { createLotHandlers } from './lotHandlers';
import { createMasterDataHandlers } from './masterDataHandlers';
import { createReportHandlers } from './reportHandlers';
import { createSettlementHandlers } from './settlementHandlers';
import { createStockHandlers } from './stockHandlers';
import { toBagsView, toHistory } from './shipmentTabs';
import { updateRecipeHandler } from './recipeHandlers';
import { updateTransportHandler } from './transportHandlers';
import { isInScope, toDetailView, toPermittedView, toSummary } from './shipmentViews';
import { createMockUser } from './users';

const DEFAULT_PAGE_SIZE = 20;

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

type DispatchBody = {
  grossKg: Kilogram;
  tareKg: Kilogram;
  bagCount: number;
  shortfallReason?: string;
};

/** İstek gövdesini sözleşmeye göre daraltır; sahte API gerçek backend gibi 400 döner. */
const isKg = (value: unknown): value is Kilogram => typeof value === 'string' && isKilogram(value);
const isPositiveInt = (value: unknown): value is number =>
  typeof value === 'number' && Number.isInteger(value) && value >= 1;
const isIsoDay = (value: unknown): value is string =>
  typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
const isNonEmpty = (value: unknown): value is string => typeof value === 'string' && value !== '';

function readDispatchBody(json: unknown): DispatchBody | undefined {
  if (typeof json !== 'object' || json === null) return undefined;
  const { grossKg, tareKg, bagCount, shortfallReason } = json as Record<string, unknown>;
  if (!isKg(grossKg) || !isKg(tareKg)) return undefined;
  if (!isPositiveInt(bagCount)) return undefined;
  return {
    grossKg,
    tareKg,
    bagCount,
    ...(isNonEmpty(shortfallReason) ? { shortfallReason } : {}),
  };
}

function toMockWeighing({ grossKg, tareKg, bagCount }: DispatchBody): Weighing {
  return {
    grossKg,
    tareKg,
    netKg: calculateNetKg(grossKg, tareKg) ?? grossKg,
    bagCount,
    weighedAt: new Date().toISOString(),
  };
}

const notFound = () =>
  HttpResponse.json(
    { status: 404, code: 'SHIPMENT_NOT_FOUND', title: 'Sevkiyat bulunamadı' },
    { status: 404 },
  );

export function createMeHandler(role: Role) {
  return http.get(ME_PATH, () => HttpResponse.json(createMockUser(role)));
}

/** Sunucudaki arama: numara, fasoncu, şoför ve plakada büyük-küçük harf ayırmadan; tarih aralığı iki ucu kapsar. */
function matchesShipmentFilter(shipment: InternalShipmentView, params: URLSearchParams): boolean {
  const search = (params.get('search') ?? '').trim().toLocaleLowerCase('tr');
  const from = params.get('from');
  const to = params.get('to');
  const haystack = [
    shipment.shipmentNo,
    shipment.subcontractorTitle,
    shipment.driverName ?? '',
    shipment.plateNo ?? '',
  ]
    .join(' ')
    .toLocaleLowerCase('tr');
  return (
    haystack.includes(search) &&
    (from === null || shipment.shipmentDate >= from) &&
    (to === null || shipment.shipmentDate <= to)
  );
}

function createListShipmentsHandler(role: Role) {
  const user = createMockUser(role);
  return http.get(SHIPMENTS_PATH, ({ request }) => {
    const params = new URL(request.url).searchParams;
    const page = Number(params.get('page') ?? '0');
    const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
    const statuses = params.getAll('status');
    const matching = listMockShipments().filter(
      (shipment) =>
        isInScope(shipment, user) &&
        (statuses.length === 0 || statuses.includes(shipment.status)) &&
        matchesShipmentFilter(shipment, params),
    );
    return HttpResponse.json({
      items: matching.slice(page * size, (page + 1) * size).map(toSummary),
      page: {
        number: page,
        size,
        totalElements: matching.length,
        totalPages: Math.ceil(matching.length / size),
      },
    });
  });
}

const outOfScope = () =>
  HttpResponse.json(
    { status: 403, code: 'OUT_OF_SCOPE', title: 'Erişim dışı kayıt' },
    { status: 403 },
  );

function createGetShipmentHandler(role: Role) {
  const user = createMockUser(role);
  return http.get(`${SHIPMENTS_PATH}/:shipmentId`, ({ params }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    if (!isInScope(shipment, user)) return outOfScope();
    return HttpResponse.json(toPermittedView(toDetailView(shipment, role), user.permissions));
  });
}

function createTabHandlers(role: Role) {
  const user = createMockUser(role);
  const withShipment = (shipmentId: unknown, respond: (s: InternalShipmentView) => Response) => {
    const shipment = findMockShipment(shipmentId);
    if (shipment === undefined) return notFound();
    return isInScope(shipment, user) ? respond(shipment) : outOfScope();
  };
  return [
    http.get(`${SHIPMENTS_PATH}/:shipmentId/history`, ({ params }) =>
      withShipment(params.shipmentId, (s) => HttpResponse.json({ items: toHistory(s) })),
    ),
    http.get(`${SHIPMENTS_PATH}/:shipmentId/bags`, ({ params }) =>
      withShipment(params.shipmentId, (s) => HttpResponse.json(toBagsView(s, role))),
    ),
    http.post(`${SHIPMENTS_PATH}/:shipmentId/bags`, async ({ params, request }) => {
      const body = (await request.json()) as { weightsKg?: string[] };
      const weights = body.weightsKg ?? [];
      return withShipment(params.shipmentId, (s) =>
        s.shipmentDate > todayIsoDate()
          ? problem(409, 'SHIPMENT_NOT_DUE', 'Sevkiyat günü gelmedi')
          : HttpResponse.json({ assignedCount: assignMockBagWeights(s.shipmentId, weights) }),
      );
    }),
    http.delete(`${SHIPMENTS_PATH}/:shipmentId/bags/:seqNo`, ({ params }) =>
      withShipment(params.shipmentId, () => new HttpResponse(null, { status: 204 })),
    ),
  ];
}

/** İptal: DRAFT ya da AWAITING_DELIVERY; tamamlanmış ya da iptal edilmişse 409. */
const cancelShipmentHandler = http.post(`${SHIPMENTS_PATH}/:shipmentId/cancel`, ({ params }) => {
  const shipment = findMockShipment(params.shipmentId);
  if (shipment === undefined) return notFound();
  if (
    shipment.status !== 'DRAFT' &&
    shipment.status !== 'AWAITING_DELIVERY' &&
    shipment.status !== 'WEIGHT_MISMATCH'
  ) {
    return HttpResponse.json({ code: 'INVALID_SHIPMENT_STATUS' }, { status: 409 });
  }
  updateMockShipmentStatus(shipment.shipmentId, 'CANCELLED');
  return new HttpResponse(null, { status: 204 });
});

/** Sevkiyat günü: yalnızca taslakta ve bugünden önce olmayan bir güne değişir. */
const updateShipmentDateHandler = http.put(
  `${SHIPMENTS_PATH}/:shipmentId/shipment-date`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    const body = (await request.json()) as { shipmentDate?: unknown };
    if (!isIsoDay(body.shipmentDate) || body.shipmentDate < todayIsoDate()) {
      return problem(400, 'INVALID_SHIPMENT', 'Geçersiz sevkiyat günü');
    }
    if (shipment.status !== 'DRAFT') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    updateMockShipmentDate(shipment.shipmentId, body.shipmentDate);
    return new HttpResponse(null, { status: 204 });
  },
);

/** İrsaliyeyle tamamlama: AWAITING_DELIVERY → COMPLETED; numara ya da kanıt eksikse 400. */
const completeShipmentHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/completion`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    const json: unknown = await request.json();
    const { waybillNo, waybillEvidenceId } =
      typeof json === 'object' && json !== null ? (json as Record<string, unknown>) : {};
    if (!isNonEmpty(waybillNo) || !isNonEmpty(waybillEvidenceId)) {
      return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
    }
    if (shipment.status !== 'AWAITING_DELIVERY') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    completeMockShipment(shipment.shipmentId, waybillNo.trim());
    return new HttpResponse(null, { status: 204 });
  },
);

function dispatchPrecondition(shipment: InternalShipmentView): Response | undefined {
  if (shipment.status !== 'DRAFT') return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
  return shipment.shipmentDate > todayIsoDate()
    ? problem(409, 'SHIPMENT_NOT_DUE', 'Sevkiyat günü gelmedi')
    : undefined;
}

/** Depo çıkış tartısı: DRAFT → AWAITING_DELIVERY. */
const dispatchWeighingHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/dispatch-weighing`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    const body = readDispatchBody(await request.json());
    if (body === undefined) return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
    const blocked = dispatchPrecondition(shipment);
    if (blocked !== undefined) return blocked;
    const weighing = toMockWeighing(body);
    const mismatch = bagMismatchResponse(shipment.shipmentId, weighing);
    if (mismatch !== undefined) return mismatch;
    const expected = shipment.plannedNetKg;
    const diff = expected === undefined ? undefined : subtractKg(weighing.netKg, expected);
    if (expected !== undefined && diff !== undefined && !diff.eq(0)) {
      recordMockDispatch(shipment.shipmentId, weighing, {
        expectedKg: expected,
        measuredKg: weighing.netKg,
        diffKg: diff.toFixed(3),
        ...(body.shortfallReason === undefined ? {} : { shortfallReason: body.shortfallReason }),
      } satisfies WeightCheckView);
    } else {
      recordMockDispatch(shipment.shipmentId, weighing);
    }
    return new HttpResponse(null, { status: 204 });
  },
);

const MAX_FIRE_RATE = 0.2;
const FIRE_RATE_PATTERN = /^0\.\d{1,4}$|^[01]$/;

function readFireRate(value: unknown): string | undefined {
  return typeof value === 'string' &&
    FIRE_RATE_PATTERN.test(value) &&
    Number(value) <= MAX_FIRE_RATE
    ? value
    : undefined;
}

function readRecipeLine(item: unknown): RecipeLineInput | undefined {
  const line = typeof item === 'object' && item !== null ? (item as Record<string, unknown>) : {};
  const { yarnTypeId, lotId, netKg } = line;
  if (!isKg(netKg) || Number(netKg) <= 0) return undefined;
  if (!isNonEmpty(yarnTypeId)) return undefined;
  return {
    netKg,
    yarnTypeId,
    ...(isNonEmpty(lotId) ? { lotId } : {}),
  };
}

/** En az bir satır; her satırda Tanımlar'dan seçilmiş iplik cinsi ve 0'dan büyük net kg. */
function readRecipeLines(value: unknown): RecipeLineInput[] | undefined {
  if (!Array.isArray(value) || value.length === 0) return undefined;
  const lines = (value as unknown[]).map(readRecipeLine);
  return lines.every((line) => line !== undefined) ? lines : undefined;
}

const DECISIONS: readonly WeightDecisionKind[] = ['CANCEL', 'ADD_REMOVE_BAGS'];

function readDecisionBody(
  json: unknown,
): { decision: WeightDecisionKind; reason: string } | undefined {
  const { decision, reason } =
    typeof json === 'object' && json !== null ? (json as Record<string, unknown>) : {};
  const kind = DECISIONS.find((candidate) => candidate === decision);
  if (kind === undefined) return undefined;
  return isNonEmpty(reason) && reason.trim() !== '' ? { decision: kind, reason } : undefined;
}

/** Kantar kararı: yalnız WEIGHT_MISMATCH durumunda ve gerekçeyle. */
const weightDecisionHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/weight-decision`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    const body = readDecisionBody(await request.json());
    if (body === undefined) return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
    if (shipment.status !== 'WEIGHT_MISMATCH') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    decideMockWeight(shipment.shipmentId, body.decision, body.reason);
    return new HttpResponse(null, { status: 204 });
  },
);

const fireRateHandler = http.get('/api/v1/settings/shipment', () =>
  HttpResponse.json({ defaultFireRate: '0.0400', weightToleranceKg: '0.000' }),
);

/** Gönderen fasoncu kimlik ve unvanıyla birlikte geldiyse isteğe eklenir. */
function readSource(body: Record<string, unknown>) {
  const { sourceSubcontractorId, sourceSubcontractorTitle } = body;
  return isNonEmpty(sourceSubcontractorId) && isNonEmpty(sourceSubcontractorTitle)
    ? { sourceSubcontractorId, sourceSubcontractorTitle }
    : {};
}

/** Reçete, fire oranı ve sevkiyat günü; biri geçersizse `undefined`. */
function readCreateDetails(body: Record<string, unknown>) {
  const lines = readRecipeLines(body.lines);
  const fireRate = readFireRate(body.fireRate);
  const { shipmentDate } = body;
  if (lines === undefined || fireRate === undefined || !isIsoDay(shipmentDate)) return undefined;
  return { lines, fireRate, shipmentDate };
}

/** Sevkiyat oluşturma: zorunlu alanlar eksikse 400, aksi halde 201 ve yeni numara. */
const createShipmentHandler = http.post(SHIPMENTS_PATH, async ({ request }) => {
  const json: unknown = await request.json();
  const body = typeof json === 'object' && json !== null ? (json as Record<string, unknown>) : {};
  const { siteId, subcontractorId, subcontractorTitle } = body;
  if (!isNonEmpty(siteId) || !isNonEmpty(subcontractorId) || !isNonEmpty(subcontractorTitle)) {
    return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
  }
  const { driverName, plateNo } = body;
  const details = readCreateDetails(body);
  if (details === undefined) return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
  const { lines, fireRate, shipmentDate } = details;
  const created = addMockShipment({
    siteId,
    subcontractorId,
    subcontractorTitle,
    lines,
    fireRate,
    shipmentDate,
    ...readSource(body),
    ...(isNonEmpty(driverName) ? { driverName } : {}),
    ...(isNonEmpty(plateNo) ? { plateNo } : {}),
  });
  return HttpResponse.json(
    { id: created.shipmentId, shipmentNo: created.shipmentNo },
    { status: 201 },
  );
});

/** Oturum uçları: sahte API her girişi kabul eder; çıkış 204 döner. */
const logoutHandler = http.post(
  '/api/v1/auth/logout',
  () => new HttpResponse(null, { status: 204 }),
);
const loginHandler = http.post('/api/v1/auth/login', () =>
  HttpResponse.json({ passwordChangeRequired: false }),
);
/** Şifre talebi: giriş istemez, cevap her durumda 204'tür (hesabın varlığı sızmaz). */
const passwordRequestHandler = http.post('/api/v1/auth/password-requests', async ({ request }) => {
  const body = (await request.json()) as { username?: unknown };
  if (typeof body.username !== 'string' || body.username.trim() === '') {
    return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
  }
  requestMockPassword(body.username);
  return new HttpResponse(null, { status: 204 });
});
const changeOwnPasswordHandler = http.put(
  '/api/v1/auth/password',
  () => new HttpResponse(null, { status: 204 }),
);

export function createHandlers(role: Role) {
  return [
    createMeHandler(role),
    createListShipmentsHandler(role),
    createGetShipmentHandler(role),
    cancelShipmentHandler,
    updateShipmentDateHandler,
    updateTransportHandler,
    updateRecipeHandler,
    completeShipmentHandler,
    dispatchWeighingHandler,
    weightDecisionHandler,
    fireRateHandler,
    ...createTabHandlers(role),
    createShipmentHandler,
    ...createMasterDataHandlers(createMockUser(role).permissions),
    ...createBrandingHandlers(createMockUser(role).permissions),
    ...createRolePermissionHandlers(createMockUser(role).permissions),
    ...createLotHandlers(createMockUser(role).permissions),
    ...createLoginAccountHandlers(createMockUser(role).permissions),
    ...createEvidenceHandlers(createMockUser(role).permissions),
    ...createDiscrepancyHandlers(createMockUser(role).permissions, createMockUser(role).userId),
    ...createClaimHandlers(role),
    ...createAuditHandlers(createMockUser(role).permissions),
    ...createReportHandlers(createMockUser(role).permissions),
    ...createSettlementHandlers(createMockUser(role)),
    ...createStockHandlers(createMockUser(role)),
    logoutHandler,
    loginHandler,
    passwordRequestHandler,
    changeOwnPasswordHandler,
  ];
}
