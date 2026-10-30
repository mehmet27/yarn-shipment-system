import type {
  DeliveryScanView,
  DispatchBagView,
  InternalShipmentView,
  Role,
  ShipmentBagsResponse,
  StatusChangeView,
} from '@iplik/api-client';
import { SHIPMENT_STATUSES } from '@iplik/api-client';

const HOUR_MS = 3_600_000;
/** Backend sistem adımlarını sıfır UUID ile kaydeder (ör. karşılaştırma sonrası İncelemede). */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';

/** Durum sırası boyunca (iptal hariç) taslaktan mevcut duruma saatlik geçişler üretir. */
export function toHistory(shipment: InternalShipmentView): StatusChangeView[] {
  const target = shipment.status === 'CANCELLED' ? 0 : SHIPMENT_STATUSES.indexOf(shipment.status);
  const start = new Date(shipment.createdAt).getTime();
  return SHIPMENT_STATUSES.slice(0, target + 1).map((toStatus, index) => ({
    ...(index === 0 ? {} : { fromStatus: SHIPMENT_STATUSES[index - 1] }),
    toStatus,
    changedAt: new Date(start + index * HOUR_MS).toISOString(),
    changedBy: toStatus === 'IN_REVIEW' ? SYSTEM_ACTOR_ID : 'user-warehouse',
  }));
}

function dispatchBags(shipment: InternalShipmentView): DispatchBagView[] {
  const count = shipment.dispatchWeighing?.bagCount ?? 0;
  return Array.from({ length: count }, (_, index) => ({
    bagId: `${shipment.shipmentId}-bag-${String(index)}`,
    uniqueKey: `BAG-${shipment.shipmentId}-${String(index + 1).padStart(3, '0')}`,
    lotNo: 'LOT-2026-17',
    qualityGradeCode: 'A',
    scanMethod: 'SCAN',
    assignedAt: shipment.createdAt,
  }));
}

function deliveryScans(shipment: InternalShipmentView, isInternal: boolean): DeliveryScanView[] {
  if (shipment.deliveryWeighing === undefined) return [];
  return dispatchBags(shipment).map((bag) => ({
    scannedKey: bag.uniqueKey,
    scanMethod: 'SCAN',
    scannedAt: shipment.createdAt,
    ...(isInternal ? { matched: true } : {}),
  }));
}

/** Kör giriş: çıkış listesi yalnızca INTERNAL/WAREHOUSE, teslim okutmaları INTERNAL/SUBCONTRACTOR görür. */
export function toBagsView(shipment: InternalShipmentView, role: Role): ShipmentBagsResponse {
  switch (role) {
    case 'WAREHOUSE':
      return { audience: 'WAREHOUSE', dispatchBags: dispatchBags(shipment) };
    case 'SUBCONTRACTOR':
      return { audience: 'SUBCONTRACTOR', deliveryScans: deliveryScans(shipment, false) };
    case 'DRIVER':
      return { audience: 'DRIVER' };
    case 'ADMIN':
    case 'MANAGER':
    case 'QUALITY':
      return {
        audience: 'INTERNAL',
        dispatchBags: dispatchBags(shipment),
        deliveryScans: deliveryScans(shipment, true),
      };
  }
}
