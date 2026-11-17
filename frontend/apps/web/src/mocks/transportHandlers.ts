import { SHIPMENTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { findMockShipment, updateMockShipmentTransport } from './mockStore';

const isText = (value: unknown): value is string => typeof value === 'string' && value !== '';
const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

/** Şoför ve araç düzeltme: yalnızca taslakta; en az biri gerekir, verilmeyen alan değişmez. */
export const updateTransportHandler = http.put(
  `${SHIPMENTS_PATH}/:shipmentId/transport`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return problem(404, 'NOT_FOUND', 'Bulunamadı');
    const body = (await request.json()) as { driverName?: unknown; plateNo?: unknown };
    const driverName = isText(body.driverName) ? body.driverName : undefined;
    const plateNo = isText(body.plateNo) ? body.plateNo : undefined;
    if (driverName === undefined && plateNo === undefined) {
      return problem(400, 'VALIDATION_FAILED', 'Şoför ya da araç gerekli');
    }
    if (shipment.status !== 'DRAFT') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    updateMockShipmentTransport(shipment.shipmentId, {
      ...(driverName === undefined ? {} : { driverName }),
      ...(plateNo === undefined ? {} : { plateNo }),
    });
    return new HttpResponse(null, { status: 204 });
  },
);
