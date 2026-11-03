import type {
  CorrectionView,
  InternalShipmentView,
  RecordCorrectionRequest,
  Weighing,
} from '@iplik/api-client';
import { calculateNetKg, isKilogram } from '@iplik/units';
import { isMockSealUsed, patchMockShipment } from './mockStore';

type Outcome = { oldValue: string; newValue: string } | 'INVALID_CORRECTION' | 'SEAL_ALREADY_USED';

/** Sahte API'nin düzeltme kayıtları; backend gibi Admin'in kaydı aynı anda onaylanmış sayılır. */
let corrections = new Map<string, CorrectionView[]>();

export function listMockCorrections(shipmentId: string): CorrectionView[] {
  return corrections.get(shipmentId) ?? [];
}

const weighingText = (w: Pick<Weighing, 'grossKg' | 'tareKg' | 'netKg'>) =>
  `gross=${w.grossKg} tare=${w.tareKg} net=${w.netKg}`;

/** Tartım düzeltmesi: brüt ve dara değişir, net yeniden hesaplanır. */
function correctWeighing(
  shipment: InternalShipmentView,
  request: RecordCorrectionRequest,
): Outcome {
  const key = request.target === 'DISPATCH_WEIGHING' ? 'dispatchWeighing' : 'deliveryWeighing';
  const current = shipment[key];
  const { grossKg, tareKg } = request;
  if (current === undefined || grossKg === undefined || tareKg === undefined) {
    return 'INVALID_CORRECTION';
  }
  if (!isKilogram(grossKg) || !isKilogram(tareKg)) return 'INVALID_CORRECTION';
  const netKg = calculateNetKg(grossKg, tareKg);
  if (netKg === undefined) return 'INVALID_CORRECTION';
  const next = { ...current, grossKg, tareKg, netKg };
  const outcome = { oldValue: weighingText(current), newValue: weighingText(next) };
  if (outcome.oldValue === outcome.newValue) return 'INVALID_CORRECTION';
  patchMockShipment(shipment.shipmentId, { [key]: next });
  return outcome;
}

/** Çıkış mühür numarası düzeltmesi; teslim mührü sahte veride tutulmaz. */
function correctSeal(shipment: InternalShipmentView, request: RecordCorrectionRequest): Outcome {
  const oldValue = request.target === 'DISPATCH_SEAL_NO' ? shipment.dispatchSealNo : undefined;
  const newValue = request.sealNo?.trim();
  if (oldValue === undefined || newValue === undefined || newValue === oldValue) {
    return 'INVALID_CORRECTION';
  }
  if (isMockSealUsed(newValue, shipment.shipmentId)) return 'SEAL_ALREADY_USED';
  patchMockShipment(shipment.shipmentId, { dispatchSealNo: newValue });
  return { oldValue, newValue };
}

/** Hata kodu ya da kaydedilen düzeltme. */
export function recordMockCorrection(
  shipment: InternalShipmentView,
  request: RecordCorrectionRequest,
  userId: string,
): CorrectionView | Exclude<Outcome, object> {
  if (shipment.status === 'CANCELLED') return 'INVALID_CORRECTION';
  const isWeighing =
    request.target === 'DISPATCH_WEIGHING' || request.target === 'DELIVERY_WEIGHING';
  const outcome = isWeighing ? correctWeighing(shipment, request) : correctSeal(shipment, request);
  if (typeof outcome === 'string') return outcome;
  const view: CorrectionView = {
    id: crypto.randomUUID(),
    target: request.target,
    ...outcome,
    reason: request.reason,
    status: 'APPROVED',
    requestedBy: userId,
    approvedBy: userId,
    createdAt: new Date().toISOString(),
  };
  corrections.set(shipment.shipmentId, [...listMockCorrections(shipment.shipmentId), view]);
  return view;
}

export function resetMockCorrections(): void {
  corrections = new Map();
}
