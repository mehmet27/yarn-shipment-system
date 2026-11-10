import type { CorrectionTarget, Permission, RecordCorrectionRequest } from '@iplik/api-client';
import { CorrectionTarget as TARGETS, SHIPMENTS_PATH } from '@iplik/api-client';
import { isKilogram } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { listMockCorrections, recordMockCorrection } from './corrections';
import { findMockShipment } from './mockStore';

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const optionalKg = (value: unknown) =>
  typeof value === 'string' && isKilogram(value) ? value : undefined;

function readCorrection(json: unknown): RecordCorrectionRequest | undefined {
  if (!isRecord(json) || typeof json.reason !== 'string' || json.reason.trim() === '') {
    return undefined;
  }
  const target: CorrectionTarget | undefined = Object.values(TARGETS).find(
    (value) => value === json.target,
  );
  if (target === undefined) return undefined;
  const grossKg = optionalKg(json.grossKg);
  const tareKg = optionalKg(json.tareKg);
  return {
    target,
    reason: json.reason,
    ...(grossKg === undefined ? {} : { grossKg }),
    ...(tareKg === undefined ? {} : { tareKg }),
  };
}

/** Gerekçeli düzeltme; backend gibi yazma CORRECTION_CREATE, okuma ayrıca AUDIT_VIEW ile. */
export function createCorrectionHandlers(permissions: readonly Permission[], userId: string) {
  const canCorrect = permissions.includes('CORRECTION_CREATE');
  const canRead = canCorrect || permissions.includes('AUDIT_VIEW');
  const path = `${SHIPMENTS_PATH}/:shipmentId/corrections`;
  return [
    http.get(path, ({ params }) => {
      if (!canRead) return problem(403, 'ACCESS_DENIED');
      const shipment = findMockShipment(params.shipmentId);
      if (shipment === undefined) return problem(404, 'SHIPMENT_NOT_FOUND');
      return HttpResponse.json({ items: listMockCorrections(shipment.shipmentId) });
    }),
    http.post(path, async ({ params, request }) => {
      if (!canCorrect) return problem(403, 'ACCESS_DENIED');
      const shipment = findMockShipment(params.shipmentId);
      if (shipment === undefined) return problem(404, 'SHIPMENT_NOT_FOUND');
      const body = readCorrection(await request.json());
      if (body === undefined) return problem(400, 'INVALID_REQUEST');
      const result = recordMockCorrection(shipment, body, userId);
      if (result === 'INVALID_CORRECTION') return problem(400, result);
      return HttpResponse.json(result, { status: 201 });
    }),
  ];
}
