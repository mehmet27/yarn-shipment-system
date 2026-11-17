import type {
  InternalShipmentView,
  WeightDecisionKind,
  WeightDecisionLine,
} from '@iplik/api-client';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import { sumKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { decideMockWeight, findMockShipment } from './mockStore';

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

const isNonEmpty = (value: unknown): value is string => typeof value === 'string' && value !== '';

const DECISIONS: readonly WeightDecisionKind[] = ['CANCEL', 'ADD_REMOVE_BAGS', 'UPDATE_RECIPE'];

type DecisionBody = {
  decision: WeightDecisionKind;
  reason: string;
  lines: WeightDecisionLine[];
};

function readDecisionBody(json: unknown): DecisionBody | undefined {
  const { decision, reason, lines } =
    typeof json === 'object' && json !== null ? (json as Record<string, unknown>) : {};
  const kind = DECISIONS.find((candidate) => candidate === decision);
  if (kind === undefined) return undefined;
  const readLines = Array.isArray(lines) ? (lines as WeightDecisionLine[]) : [];
  return isNonEmpty(reason) && reason.trim() !== ''
    ? { decision: kind, reason, lines: readLines }
    : undefined;
}

/** Düzeltilen iplik kiloları her satırı kapsar ve toplamı tartılan net'e eşittir (sunucu kuralı). */
function isMatchingCorrection(
  shipment: InternalShipmentView,
  lines: readonly WeightDecisionLine[],
): boolean {
  const measured = shipment.weightCheck?.measuredKg;
  const isCovered = (shipment.lines ?? []).every((line) =>
    lines.some((candidate) => candidate.lineNo === line.lineNo),
  );
  return measured !== undefined && isCovered && sumKg(lines.map((line) => line.netKg)) === measured;
}

/** Kantar kararı: yalnız WEIGHT_MISMATCH durumunda ve gerekçeyle. */
export const weightDecisionHandler = http.post(
  `${SHIPMENTS_PATH}/:shipmentId/weight-decision`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return problem(404, 'SHIPMENT_NOT_FOUND', 'Sevkiyat bulunamadı');
    const body = readDecisionBody(await request.json());
    if (body === undefined) return problem(400, 'VALIDATION_FAILED', 'Geçersiz istek');
    if (shipment.status !== 'WEIGHT_MISMATCH') {
      return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    }
    if (body.decision === 'UPDATE_RECIPE' && !isMatchingCorrection(shipment, body.lines)) {
      return problem(400, 'INVALID_WEIGHT_DECISION', 'Geçersiz iplik listesi');
    }
    decideMockWeight(shipment.shipmentId, {
      kind: body.decision,
      reason: body.reason,
      lines: body.lines,
    });
    return new HttpResponse(null, { status: 204 });
  },
);
