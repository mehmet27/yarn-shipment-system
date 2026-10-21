import type { Role, ShipmentDetail } from '@iplik/api-client';
import { ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { MOCK_SHIPMENTS } from './shipments';
import { createMockUser } from './users';

const DEFAULT_PAGE_SIZE = 20;
const COMPARED_STATUSES = new Set(['IN_REVIEW', 'COMPLETED']);

const notFound = () =>
  HttpResponse.json(
    { status: 404, code: 'RESOURCE_NOT_FOUND', title: 'Kayıt bulunamadı' },
    { status: 404 },
  );

/** FE-TEST-05: kör giriş; fasoncu karşılaştırma bitene kadar çıkış alanlarını hiç almaz. */
function toRoleView(shipment: ShipmentDetail, role: Role): ShipmentDetail {
  const isSubcontractor = createMockUser(role).permissions.includes('DELIVERY_RECORD');
  if (!isSubcontractor || COMPARED_STATUSES.has(shipment.status)) return shipment;
  const { dispatchWeighing, sealNo, ...blindView } = shipment;
  return blindView;
}

export function createMeHandler(role: Role) {
  return http.get(ME_PATH, () => HttpResponse.json(createMockUser(role)));
}

export const unauthenticatedMeHandler = http.get(ME_PATH, () =>
  HttpResponse.json(
    { status: 401, code: 'UNAUTHENTICATED', title: 'Oturum açmanız gerekiyor' },
    { status: 401 },
  ),
);

const listShipmentsHandler = http.get(SHIPMENTS_PATH, ({ request }) => {
  const params = new URL(request.url).searchParams;
  const page = Number(params.get('page') ?? '0');
  const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
  const status = params.get('status');
  const matching = MOCK_SHIPMENTS.filter(
    (shipment) => status === null || shipment.status === status,
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

function createGetShipmentHandler(role: Role) {
  return http.get(`${SHIPMENTS_PATH}/:shipmentId`, ({ params }) => {
    const shipment = MOCK_SHIPMENTS.find((candidate) => candidate.shipmentId === params.shipmentId);
    return shipment === undefined ? notFound() : HttpResponse.json(toRoleView(shipment, role));
  });
}

export function createHandlers(role: Role) {
  return [createMeHandler(role), listShipmentsHandler, createGetShipmentHandler(role)];
}
