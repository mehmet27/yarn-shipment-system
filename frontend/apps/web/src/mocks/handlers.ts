import type { Role } from '@iplik/api-client';
import { ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { findMockShipment, listMockShipments, updateMockShipmentStatus } from './mockStore';
import { isInScope, toDetailView, toSummary } from './shipmentViews';
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

/** Gateway çıkışı: gerçek yanıt Keycloak çıkış adresidir; sahte API ana sayfaya döner. */
const logoutHandler = http.post('/logout', () => HttpResponse.json({ logoutUrl: '/' }));

export function createHandlers(role: Role) {
  return [
    createMeHandler(role),
    createListShipmentsHandler(role),
    createGetShipmentHandler(role),
    confirmPickupHandler,
    deliveryReceiptHandler,
    logoutHandler,
  ];
}
