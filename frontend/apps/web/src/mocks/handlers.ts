import type { Role, Weighing } from '@iplik/api-client';
import { ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, isKilogram } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import {
  addMockShipment,
  findMockShipment,
  isMockSealUsed,
  listMockShipments,
  recordMockDispatch,
  updateMockShipmentStatus,
} from './mockStore';
import {
  matchesQuery,
  MOCK_DRIVERS,
  MOCK_SITES,
  MOCK_SUBCONTRACTORS,
  MOCK_VEHICLES,
} from './masterData';
import { isInScope, toDetailView, toSummary } from './shipmentViews';
import { createMockUser } from './users';

const DEFAULT_PAGE_SIZE = 20;

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

type DispatchBody = { grossKg: Kilogram; tareKg: Kilogram; bagCount: number; sealNo: string };

/** İstek gövdesini sözleşmeye göre daraltır; sahte API gerçek backend gibi 400 döner. */
const isKg = (value: unknown): value is Kilogram => typeof value === 'string' && isKilogram(value);
const isPositiveInt = (value: unknown): value is number =>
  typeof value === 'number' && Number.isInteger(value) && value >= 1;
const isNonEmpty = (value: unknown): value is string => typeof value === 'string' && value !== '';

function readDispatchBody(json: unknown): DispatchBody | undefined {
  if (typeof json !== 'object' || json === null) return undefined;
  const { grossKg, tareKg, bagCount, sealNo } = json as Record<string, unknown>;
  if (!isKg(grossKg) || !isKg(tareKg)) return undefined;
  return isPositiveInt(bagCount) && isNonEmpty(sealNo)
    ? { grossKg, tareKg, bagCount, sealNo }
    : undefined;
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

export const unauthenticatedMeHandler = http.get(ME_PATH, () =>
  HttpResponse.json(
    { status: 401, code: 'AUTHENTICATION_REQUIRED', title: 'Oturum açmanız gerekiyor' },
    { status: 401 },
  ),
);

function createListShipmentsHandler(role: Role) {
  const user = createMockUser(role);
  return http.get(SHIPMENTS_PATH, ({ request }) => {
    const params = new URL(request.url).searchParams;
    const page = Number(params.get('page') ?? '0');
    const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
    const statuses = params.getAll('status');
    const matching = listMockShipments().filter(
      (shipment) =>
        isInScope(shipment, user) && (statuses.length === 0 || statuses.includes(shipment.status)),
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
    return HttpResponse.json(toDetailView(shipment, role));
  });
}

/** Geçici uç: şoförün "yükü teslim aldım" beyanı; LOADED → IN_TRANSIT. */
const confirmPickupHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/pickup-confirmation`,
  ({ params }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    updateMockShipmentStatus(shipment.shipmentId, 'IN_TRANSIT');
    return new HttpResponse(null, { status: 204 });
  },
);

/** Kör teslim tutanağı; AWAITING_DELIVERY → CHECKING, aksi halde 409 INVALID_SHIPMENT_STATUS. */
const deliveryReceiptHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/delivery-receipt`,
  ({ params }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    if (shipment.status !== 'AWAITING_DELIVERY') {
      return HttpResponse.json(
        { status: 409, code: 'INVALID_SHIPMENT_STATUS', title: 'Geçersiz durum' },
        { status: 409 },
      );
    }
    updateMockShipmentStatus(shipment.shipmentId, 'CHECKING');
    return new HttpResponse(null, { status: 204 });
  },
);

/** Depo çıkış tartısı: DRAFT → LOADED; mühür başka sevkiyatta kullanılmışsa 409 SEAL_ALREADY_USED. */
const dispatchWeighingHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/dispatch-weighing`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return notFound();
    const body = readDispatchBody(await request.json());
    if (body === undefined) return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
    if (shipment.status !== 'DRAFT') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    if (isMockSealUsed(body.sealNo, shipment.shipmentId)) {
      return problem(409, 'SEAL_ALREADY_USED', 'Mühür kullanılmış');
    }
    recordMockDispatch(shipment.shipmentId, toMockWeighing(body), body.sealNo);
    return new HttpResponse(null, { status: 204 });
  },
);

/** Ana veri liste uçları: `q` araması ve sayfalama gerçek sözleşmeyle aynıdır. */
type MasterDataSource<T> = {
  path: string;
  rows: readonly T[];
  searchText: (row: T) => string;
  isInScopeRow?: (row: T, params: URLSearchParams) => boolean;
};

function masterDataHandler<T>({
  path,
  rows,
  searchText,
  isInScopeRow = () => true,
}: MasterDataSource<T>) {
  return http.get(`/api/v1/master-data/${path}`, ({ request }) => {
    const params = new URL(request.url).searchParams;
    const page = Number(params.get('page') ?? '0');
    const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
    const matching = rows.filter(
      (row) => matchesQuery(searchText(row), params.get('q')) && isInScopeRow(row, params),
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
}

const masterDataHandlers = [
  masterDataHandler({
    path: 'subcontractors',
    rows: MOCK_SUBCONTRACTORS,
    searchText: (row) => row.title,
  }),
  masterDataHandler({
    path: 'drivers',
    rows: MOCK_DRIVERS,
    searchText: (row) => row.fullName,
    isInScopeRow: (row, params) => {
      const subcontractorId = params.get('subcontractorId');
      return subcontractorId === null || row.subcontractorId === subcontractorId;
    },
  }),
  masterDataHandler({ path: 'vehicles', rows: MOCK_VEHICLES, searchText: (row) => row.plateNo }),
  masterDataHandler({ path: 'sites', rows: MOCK_SITES, searchText: (row) => row.name }),
];

/** Sevkiyat oluşturma: zorunlu alanlar eksikse 400, aksi halde 201 ve yeni numara. */
const createShipmentHandler = http.post(SHIPMENTS_PATH, async ({ request }) => {
  const json: unknown = await request.json();
  const body = typeof json === 'object' && json !== null ? (json as Record<string, unknown>) : {};
  const { siteId, subcontractorId, subcontractorTitle } = body;
  if (!isNonEmpty(siteId) || !isNonEmpty(subcontractorId) || !isNonEmpty(subcontractorTitle)) {
    return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
  }
  const { driverName, plateNo } = body;
  const created = addMockShipment({
    siteId,
    subcontractorId,
    subcontractorTitle,
    ...(isNonEmpty(driverName) ? { driverName } : {}),
    ...(isNonEmpty(plateNo) ? { plateNo } : {}),
  });
  return HttpResponse.json(
    { id: created.shipmentId, shipmentNo: created.shipmentNo },
    { status: 201 },
  );
});

/** Gateway çıkışı: gerçek yanıt Keycloak çıkış adresidir; sahte API ana sayfaya döner. */
const logoutHandler = http.post('/logout', () => HttpResponse.json({ logoutUrl: '/' }));

export function createHandlers(role: Role) {
  return [
    createMeHandler(role),
    createListShipmentsHandler(role),
    createGetShipmentHandler(role),
    confirmPickupHandler,
    deliveryReceiptHandler,
    dispatchWeighingHandler,
    createShipmentHandler,
    ...masterDataHandlers,
    logoutHandler,
  ];
}
