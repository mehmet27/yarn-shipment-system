import type { InternalShipmentView, ShipmentStatus, Weighing } from '@iplik/api-client';
import { SHIPMENT_STATUSES } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, isKilogram } from '@iplik/units';

const SHIPMENT_COUNT = 45;
const SUBCONTRACTORS = ['Aydın Örme', 'Bursa Tekstil', 'Çorlu Boya'] as const;
const DRIVERS = ['Şahin Şoför', 'Mert Kaya', 'Ali Demir'] as const;
/** Döngüsel örnek durumlar; kantar uyuşmazlığı yalnız özel örnekte (MISMATCH_INDEX) vardır. */
const CYCLE_STATUSES = SHIPMENT_STATUSES.filter((status) => status !== 'WEIGHT_MISMATCH');
const DISPATCHED: ReadonlySet<ShipmentStatus> = new Set(['AWAITING_DELIVERY', 'COMPLETED']);
export const MISMATCH_INDEX = SHIPMENT_COUNT - 1;

function kg(value: string): Kilogram {
  if (!isKilogram(value)) throw new RangeError(`Invalid kilogram value=${value}`);
  return value;
}

function weighing(grossKg: string, tareKg: string, bagCount: number): Weighing {
  const gross = kg(grossKg);
  const tare = kg(tareKg);
  return {
    grossKg: gross,
    tareKg: tare,
    netKg: calculateNetKg(gross, tare) ?? kg('0'),
    bagCount,
    weighedAt: '2026-10-20T08:15:30Z',
  };
}

function pick<T>(values: readonly T[], index: number): T {
  const value = values[index % values.length];
  if (value === undefined) throw new RangeError('Empty fixture list');
  return value;
}

/** Backend ShipmentAccessPolicy'nin durum tablosu; izne göre süzme toPermittedView'dadır. */
export function allowedActionsOf(status: ShipmentStatus): InternalShipmentView['allowedActions'] {
  switch (status) {
    case 'DRAFT':
      return ['ASSIGN_BAGS', 'RECORD_DISPATCH', 'CANCEL'];
    case 'WEIGHT_MISMATCH':
      return ['CANCEL', 'DECIDE_WEIGHT_MISMATCH'];
    case 'AWAITING_DELIVERY':
      return ['COMPLETE', 'CANCEL'];
    case 'COMPLETED':
      return ['OPEN_QUALITY_CLAIM', 'CREATE_CORRECTION'];
    case 'CANCELLED':
      return [];
  }
}

function createShipment(index: number): InternalShipmentView {
  const status = index === MISMATCH_INDEX ? 'WEIGHT_MISMATCH' : pick(CYCLE_STATUSES, index);
  const no = String(index + 1).padStart(4, '0');
  const dispatch = weighing('2000.000', '17.500', 40);
  const mismatch = weighing('1987.500', '17.500', 40);
  return {
    audience: 'INTERNAL',
    shipmentId: `shp-${no}`,
    shipmentNo: `SVK-2026-${no}`,
    siteId: 'site-merkez',
    status,
    subcontractorId: `sub-${String(index % SUBCONTRACTORS.length)}`,
    subcontractorTitle: pick(SUBCONTRACTORS, index),
    driverName: pick(DRIVERS, index),
    plateNo: `34 ABC ${no}`,
    plannedBagCount: 40,
    plannedNetKg: kg('1982.500'),
    lines: [
      { yarnTypeId: 'prd-1', yarnTypeName: '36/1 Viskon', netKg: kg('1500.000') },
      { yarnTypeText: '20 denye likra', netKg: kg('482.500') },
    ],
    fireRate: 0.04,
    createdAt: new Date(Date.UTC(2026, 9, 20, 6) - index * 3_600_000).toISOString(),
    allowedActions: allowedActionsOf(status),
    ...(DISPATCHED.has(status) && status !== 'CANCELLED' ? { dispatchWeighing: dispatch } : {}),
    ...(status === 'WEIGHT_MISMATCH' ? { dispatchWeighing: mismatch } : {}),
    ...(status === 'COMPLETED' ? { waybillNo: `IRS-${no}` } : {}),
    ...(status === 'WEIGHT_MISMATCH'
      ? {
          weightCheck: {
            expectedKg: '1982.500',
            measuredKg: '1970.000',
            diffKg: '-12.500',
            shortfallReason: 'Stok yetmedi',
          },
        }
      : {}),
  };
}

export const MOCK_SHIPMENTS: readonly InternalShipmentView[] = Array.from(
  { length: SHIPMENT_COUNT },
  (_, index) => createShipment(index),
);
