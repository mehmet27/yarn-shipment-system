import type { Role } from '@iplik/api-client';
import { ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { toDetailView, toSummary } from './shipmentViews';
import { MOCK_SHIPMENTS } from './shipments';
import { createMockUser } from './users';

const DEFAULT_PAGE_SIZE = 20;

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

const listShipmentsHandler = http.get(SHIPMENTS_PATH, ({ request }) => {
  const params = new URL(request.url).searchParams;
  const page = Number(params.get('page') ?? '0');
  const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
  const statuses = params.getAll('status');
  const matching = MOCK_SHIPMENTS.filter(
    (shipment) => statuses.length === 0 || statuses.includes(shipment.status),
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

function createGetShipmentHandler(role: Role) {
  return http.get(`${SHIPMENTS_PATH}/:shipmentId`, ({ params }) => {
    const shipment = MOCK_SHIPMENTS.find((candidate) => candidate.shipmentId === params.shipmentId);
    return shipment === undefined ? notFound() : HttpResponse.json(toDetailView(shipment, role));
  });
}

export function createHandlers(role: Role) {
  return [createMeHandler(role), listShipmentsHandler, createGetShipmentHandler(role)];
}
