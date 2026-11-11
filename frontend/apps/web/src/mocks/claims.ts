import type { Kilogram } from '@iplik/units';
import type {
  InspectionKind,
  InspectionView,
  OpenQualityClaimRequest,
  QualityClaimView,
} from '@iplik/api-client';

const DAY_MS = 86_400_000;
const CLAIM_WINDOW_DAYS = 3;
/** Sahte veride sevkiyatın net ağırlığı (kg); itiraz bunu aşamaz. */
export const MOCK_SHIPMENT_NET_KG = '1982.500' as Kilogram;

type Seed = { shipmentId: string; discrepancyId: string; claimedKg: string };

/** shp-0019 açık itirazlı (kalite), shipment-3 kapanmış kalite uyuşmazlığı disc-3'ün itirazı. */
const SEEDS: readonly Seed[] = [
  { shipmentId: 'shp-0019', discrepancyId: 'disc-claim-0019', claimedKg: '120.000' },
  { shipmentId: 'shipment-3', discrepancyId: 'disc-3', claimedKg: '45.500' },
];

let claims: QualityClaimView[] = [];

function seed({ shipmentId, discrepancyId, claimedKg }: Seed, index: number): QualityClaimView {
  const openedAt = new Date(Date.parse('2026-10-26T09:00:00Z') + index * DAY_MS).toISOString();
  return {
    id: `claim-${String(index)}`,
    discrepancyId,
    discrepancyNo: `UY-${shipmentId}-1`,
    shipmentId,
    status: discrepancyId === 'disc-3' ? 'RESOLVED' : 'OPEN',
    description: 'Kumaşta leke ve nem var.',
    claimedKg,
    evidenceIds: ['photo-seed'],
    openedBy: 'user-quality',
    openedAt,
    deadlineAt: new Date(Date.parse(openedAt) + 5 * DAY_MS).toISOString(),
    inspections: [],
  };
}

export function resetMockClaims(): void {
  claims = SEEDS.map(seed);
}
resetMockClaims();

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
