import type {
  InspectionKind,
  InspectionView,
  OpenQualityClaimRequest,
  QualityClaimView,
} from '@iplik/api-client';

const DAY_MS = 86_400_000;
const CLAIM_WINDOW_DAYS = 3;
/** Teslimde okutulan anahtarlar; sahte veride her teslim edilmiş sevkiyat için aynıdır. */
const DELIVERED_KEYS = ['BAG-0001', 'BAG-0002', 'BAG-0003'] as const;

type Seed = { shipmentId: string; discrepancyId: string; bagKey: string };

/** shp-0013 açık itirazlı (fasoncu sub-0), shipment-3 kapanmış kalite uyuşmazlığı disc-3'ün itirazı. */
const SEEDS: readonly Seed[] = [
  { shipmentId: 'shp-0013', discrepancyId: 'disc-claim-0013', bagKey: 'BAG-0003' },
  { shipmentId: 'shipment-3', discrepancyId: 'disc-3', bagKey: 'BAG-0001' },
];

let claims: QualityClaimView[] = [];

function seed({ shipmentId, discrepancyId, bagKey }: Seed, index: number): QualityClaimView {
  const openedAt = new Date(Date.parse('2026-10-26T09:00:00Z') + index * DAY_MS).toISOString();
  return {
    id: `claim-${String(index)}`,
    discrepancyId,
    discrepancyNo: `UY-${shipmentId}-1`,
    shipmentId,
    status: discrepancyId === 'disc-3' ? 'RESOLVED' : 'OPEN',
    description: 'Çuvalda leke ve nem var.',
    bagKeys: [bagKey],
    evidenceIds: ['photo-seed'],
    openedBy: 'user-subcontractor',
    openedAt,
    deadlineAt: new Date(Date.parse(openedAt) + 5 * DAY_MS).toISOString(),
    inspections: [],
  };
}

export function resetMockClaims(): void {
  claims = SEEDS.map(seed);
}
resetMockClaims();

export function mockDeliveredKeys(): string[] {
  return [...DELIVERED_KEYS];
}

export function mockDeadline(): string {
  return new Date(Date.now() + CLAIM_WINDOW_DAYS * DAY_MS).toISOString();
}

export function listMockClaims(shipmentId: string): QualityClaimView[] {
  return claims.filter((claim) => claim.shipmentId === shipmentId);
}

export function findMockClaim(id: unknown): QualityClaimView | undefined {
  return claims.find((claim) => claim.id === id);
}

export function addMockClaim(request: OpenQualityClaimRequest, actorId: string): QualityClaimView {
  const index = claims.length;
  const sameShipment = listMockClaims(request.shipmentId).length;
  const openedAt = new Date().toISOString();
  const claim: QualityClaimView = {
    ...request,
    id: `claim-${String(index)}`,
    discrepancyId: `disc-claim-${String(index)}`,
    discrepancyNo: `UY-${request.shipmentId}-${String(sameShipment + 1)}`,
    status: 'OPEN',
    openedBy: actorId,
    openedAt,
    deadlineAt: new Date(Date.parse(openedAt) + 5 * DAY_MS).toISOString(),
    inspections: [],
  };
  claims.push(claim);
  return claim;
}

export type InspectionRequest = { kind: InspectionKind; note?: string };

export function addMockInspection(
  claim: QualityClaimView,
  request: InspectionRequest,
  actorId: string,
): InspectionView {
  const inspection: InspectionView = {
    id: `${claim.id}-insp-${String(claim.inspections.length)}`,
    kind: request.kind,
    ...(request.note === undefined ? {} : { requestNote: request.note }),
    requestedBy: actorId,
    requestedAt: new Date().toISOString(),
  };
  claim.inspections.push(inspection);
  return inspection;
}

export function recordMockResult(
  inspection: InspectionView,
  result: { result: string; evidenceId?: string },
  actorId: string,
): void {
  inspection.result = result.result;
  if (result.evidenceId !== undefined) inspection.resultEvidenceId = result.evidenceId;
  inspection.performedBy = actorId;
  inspection.performedAt = new Date().toISOString();
}
