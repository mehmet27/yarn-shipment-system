import type {
  DispatchBagView,
  InternalShipmentView,
  Role,
  ShipmentStatus,
  ShipmentBagsResponse,
  StatusChangeView,
} from '@iplik/api-client';

const HOUR_MS = 3_600_000;

/** Durum sırası boyunca (iptal hariç) taslaktan mevcut duruma saatlik geçişler üretir. */
const HISTORY_ORDER: readonly ShipmentStatus[] = [
  'DRAFT',
  'AWAITING_DELIVERY',
  'COMPLETED',
  'CANCELLED',
  'WEIGHT_MISMATCH',
];

export function toHistory(shipment: InternalShipmentView): StatusChangeView[] {
  const target =
    shipment.status === 'CANCELLED' || shipment.status === 'WEIGHT_MISMATCH'
      ? 0
      : HISTORY_ORDER.indexOf(shipment.status);
  const start = new Date(shipment.createdAt).getTime();
  return HISTORY_ORDER.slice(0, target + 1).map((toStatus, index) => ({
    ...(index === 0 ? {} : { fromStatus: HISTORY_ORDER[index - 1] }),
    toStatus,
    changedAt: new Date(start + index * HOUR_MS).toISOString(),
    changedBy: toStatus === 'COMPLETED' ? 'user-manager' : 'user-warehouse',
    changedByName: toStatus === 'COMPLETED' ? 'Yusuf Müdür' : 'Deniz Depo',
  }));
}

function dispatchBags(shipment: InternalShipmentView): DispatchBagView[] {
  const count = shipment.dispatchWeighing?.bagCount ?? 0;
  return Array.from({ length: count }, (_, index) => ({
    seqNo: index + 1,
    weightKg: '50.000',
    assignedAt: shipment.createdAt,
  }));
}

/** Çıkış çuvalları her iç rolün görünümündedir. */
export function toBagsView(shipment: InternalShipmentView, role: Role): ShipmentBagsResponse {
  switch (role) {
    case 'WAREHOUSE':
      return { audience: 'WAREHOUSE', dispatchBags: dispatchBags(shipment) };
    case 'ADMIN':
    case 'MANAGER':
    case 'QUALITY':
    case 'GENERAL_MANAGER':
      return { audience: 'INTERNAL', dispatchBags: dispatchBags(shipment) };
  }
}
