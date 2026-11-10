import type {
  InspectionView,
  OpenQualityClaimRequest,
  QualityClaimView,
  Role,
} from '@iplik/api-client';
import { InspectionKind, QUALITY_CLAIMS_PATH } from '@iplik/api-client';
import { isKilogram, subtractKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import type { InspectionRequest } from './claims';
import {
  addMockClaim,
  addMockInspection,
  findMockClaim,
  listMockClaims,
  MOCK_SHIPMENT_NET_KG,
  mockDeadline,
  recordMockResult,
} from './claims';
import { findMockShipment } from './mockStore';
import { isInScope } from './shipmentViews';
import { createMockUser } from './users';

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const isStringList = (value: unknown): value is string[] =>
  Array.isArray(value) && value.every((item) => typeof item === 'string');
const isOpen = (claim: QualityClaimView) => claim.status === 'OPEN' || claim.status === 'IN_REVIEW';

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

function readInspection(json: unknown): InspectionRequest | undefined {
  if (!isRecord(json)) return undefined;
  const kind = Object.values(InspectionKind).find((value) => value === json.kind);
  if (kind === undefined) return undefined;
  return typeof json.note === 'string' ? { kind, note: json.note } : { kind };
}

type ResultBody = { result: string; evidenceId?: string };

function readResult(json: unknown): ResultBody | undefined {
  if (!isRecord(json) || typeof json.result !== 'string' || json.result === '') return undefined;
  return typeof json.evidenceId === 'string'
    ? { result: json.result, evidenceId: json.evidenceId }
    : { result: json.result };
}

/** Sonuç bir kez yazılır; lab kontrolünde rapor belgesi zorunludur (backend Inspection.recordResult). */
function resultProblem(inspection: InspectionView, body: ResultBody): Response | undefined {
  if (inspection.result !== undefined) return problem(409, 'QUALITY_CLAIM_INSPECTION_RECORDED');
  return inspection.kind === 'LAB' && body.evidenceId === undefined
    ? problem(400, 'QUALITY_CLAIM_RESULT_EVIDENCE_REQUIRED')
    : undefined;
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
  return shipment.status === 'COMPLETED'
    ? undefined
    : problem(409, 'QUALITY_CLAIM_NOT_AVAILABLE');
}

function claimProblem(request: OpenQualityClaimRequest): Response | undefined {
  if (request.evidenceIds.length === 0) return problem(400, 'QUALITY_CLAIM_EVIDENCE_REQUIRED');
  const { claimedKg } = request;
  const net = MOCK_SHIPMENT_NET_KG;
  return isKilogram(claimedKg) && subtractKg(claimedKg, net).gt(0)
    ? problem(409, 'CLAIM_EXCEEDS_SHIPMENT_WEIGHT')
    : undefined;
}

/** Kalite itirazı uçları; eylemler backend gibi izne ve duruma göre `allowedActions`'ta gelir. */
export function createClaimHandlers(role: Role) {
  const user = createMockUser(role);
  const has = (permission: (typeof user.permissions)[number]) =>
    user.permissions.includes(permission);
  const canResolve = has('DISCREPANCY_RESOLVE');
  const toView = (claim: QualityClaimView): QualityClaimView => ({
    ...claim,
    inspections: claim.inspections.map((inspection) => ({
      ...inspection,
      allowedActions: canResolve && inspection.result === undefined ? ['RECORD_RESULT'] : [],
    })),
    allowedActions: canResolve && isOpen(claim) ? ['REQUEST_INSPECTION'] : [],
  });
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
        claims: claims.map(toView),
        allowedActions: has('QUALITY_CLAIM_OPEN') ? ['OPEN_CLAIM'] : [],
      });
    }),
    http.post(QUALITY_CLAIMS_PATH, async ({ request }) => {
      if (!has('QUALITY_CLAIM_OPEN')) return problem(403, 'ACCESS_DENIED');
      const body = readOpen(await request.json());
      if (body === undefined) return problem(400, 'VALIDATION_FAILED');
      const failure = targetProblem(body.shipmentId, role) ?? claimProblem(body);
      if (failure !== undefined) return failure;
      return HttpResponse.json(toView(addMockClaim(body, user.userId)), { status: 201 });
    }),
    http.post(`${QUALITY_CLAIMS_PATH}/:claimId/inspections`, async ({ params, request }) => {
      const claim = findMockClaim(params.claimId);
      if (!canResolve) return problem(403, 'ACCESS_DENIED');
      if (claim === undefined) return problem(404, 'QUALITY_CLAIM_NOT_FOUND');
      const body = readInspection(await request.json());
      if (body === undefined) return problem(400, 'VALIDATION_FAILED');
      if (!isOpen(claim)) return problem(409, 'DISCREPANCY_NOT_OPEN');
      return HttpResponse.json(addMockInspection(claim, body, user.userId), { status: 201 });
    }),
    http.post(
      `${QUALITY_CLAIMS_PATH}/:claimId/inspections/:inspectionId/result`,
      async ({ params, request }) => {
        const inspection = findMockClaim(params.claimId)?.inspections.find(
          (item) => item.id === params.inspectionId,
        );
        if (!canResolve) return problem(403, 'ACCESS_DENIED');
        if (inspection === undefined) return problem(404, 'INSPECTION_NOT_FOUND');
        const body = readResult(await request.json());
        if (body === undefined) return problem(400, 'VALIDATION_FAILED');
        const failure = resultProblem(inspection, body);
        if (failure !== undefined) return failure;
        recordMockResult(inspection, body, user.userId);
        return HttpResponse.json(inspection);
      },
    ),
  ];
}
