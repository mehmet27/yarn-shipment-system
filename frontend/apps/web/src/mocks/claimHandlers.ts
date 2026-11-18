import type { OpenQualityClaimRequest, Role } from '@iplik/api-client';
import { QUALITY_CLAIMS_PATH } from '@iplik/api-client';
import { isKilogram, subtractKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { addMockClaim, listMockClaims, MOCK_SHIPMENT_NET_KG, mockDeadline } from './claims';
import { findMockShipment } from './mockStore';
import { isInScope } from './shipmentViews';
import { createMockUser } from './users';

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const isStringList = (value: unknown): value is string[] =>
  Array.isArray(value) && value.every((item) => typeof item === 'string');

function readOpen(json: unknown): OpenQualityClaimRequest | undefined {
  if (!isRecord(json)) return undefined;
  const { shipmentId, description, claimedKg, evidenceIds, fabricReceiptId } = json;
  if (typeof shipmentId !== 'string' || typeof description !== 'string') return undefined;
  if (typeof claimedKg !== 'string' || !isKilogram(claimedKg) || Number(claimedKg) <= 0) {
    return undefined;
  }
  if (!isStringList(evidenceIds) || description === '') return undefined;
  return {
    shipmentId,
    description,
    claimedKg,
    evidenceIds,
    ...(typeof fabricReceiptId === 'string' ? { fabricReceiptId } : {}),
  };
}

/** Tamamlanmamış ya da kapsam dışı sevkiyatın itiraz dosyası yoktur (backend ClaimAccess). */
function targetProblem(shipmentId: unknown, role: Role): Response | undefined {
  const shipment = findMockShipment(shipmentId);
  if (shipment === undefined) {
    return listMockClaims(String(shipmentId)).length > 0
      ? undefined
      : problem(409, 'QUALITY_CLAIM_NOT_AVAILABLE');
  }
  if (!isInScope(shipment, createMockUser(role))) return problem(403, 'OUT_OF_SCOPE');
  return shipment.status === 'COMPLETED' ? undefined : problem(409, 'QUALITY_CLAIM_NOT_AVAILABLE');
}

/** Girilmiş kumaştan haksız bulunmamış itirazların kilosu çıkarılır (backend ClaimTarget.claimableKg). */
function claimableOf(shipmentId: string): string {
  const claimed = listMockClaims(shipmentId)
    .filter((claim) => claim.resolution !== 'CLAIM_UNJUSTIFIED')
    .reduce((sum, claim) => sum + Number(claim.claimedKg), 0);
  return Math.max(0, Number(MOCK_SHIPMENT_NET_KG) - claimed).toFixed(3);
}

function claimProblem(request: OpenQualityClaimRequest): Response | undefined {
  if (request.evidenceIds.length === 0) return problem(400, 'QUALITY_CLAIM_EVIDENCE_REQUIRED');
  const { claimedKg } = request;
  const net = claimableOf(request.shipmentId);
  return isKilogram(claimedKg) && isKilogram(net) && subtractKg(claimedKg, net).gt(0)
    ? problem(409, 'CLAIM_EXCEEDS_SHIPMENT_WEIGHT')
    : undefined;
}

/** Kalite itirazı uçları; açma eylemi backend gibi izne göre `allowedActions`'ta gelir. */
export function createClaimHandlers(role: Role) {
  const user = createMockUser(role);
  const has = (permission: (typeof user.permissions)[number]) =>
    user.permissions.includes(permission);
  return [
    http.get(`${QUALITY_CLAIMS_PATH}/shipments/:shipmentId`, ({ params }) => {
      if (!has('DISCREPANCY_VIEW')) return problem(403, 'ACCESS_DENIED');
      const failure = targetProblem(params.shipmentId, role);
      if (failure !== undefined) return failure;
      const claims = listMockClaims(String(params.shipmentId));
      return HttpResponse.json({
        shipmentId: String(params.shipmentId),
        deadlineAt: mockDeadline(),
        windowOpen: true,
        receivedKg: MOCK_SHIPMENT_NET_KG,
        claimableKg: claimableOf(String(params.shipmentId)),
        claims: claims,
        allowedActions: has('QUALITY_CLAIM_OPEN') ? ['OPEN_CLAIM'] : [],
      });
    }),
    http.post(QUALITY_CLAIMS_PATH, async ({ request }) => {
      if (!has('QUALITY_CLAIM_OPEN')) return problem(403, 'ACCESS_DENIED');
      const body = readOpen(await request.json());
      if (body === undefined) return problem(400, 'VALIDATION_FAILED');
      const failure = targetProblem(body.shipmentId, role) ?? claimProblem(body);
      if (failure !== undefined) return failure;
      return HttpResponse.json(addMockClaim(body, user.userId), { status: 201 });
    }),
  ];
}
