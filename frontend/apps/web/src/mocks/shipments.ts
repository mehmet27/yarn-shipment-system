import type { ShipmentDetail, ShipmentStatus, Weighing } from '@iplik/api-client';
import { SHIPMENT_STATUSES } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, isKilogram } from '@iplik/units';

const SHIPMENT_COUNT = 45;
const SUBCONTRACTORS = ['Aydın Örme', 'Bursa Tekstil', 'Çorlu Boya'] as const;
const DRIVERS = ['Şahin Şoför', 'Mert Kaya', 'Ali Demir'] as const;
const DISPATCHED: ReadonlySet<ShipmentStatus> = new Set(SHIPMENT_STATUSES.slice(1));
const DELIVERED: ReadonlySet<ShipmentStatus> = new Set(['CHECKING', 'IN_REVIEW', 'COMPLETED']);

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

function createShipment(index: number): ShipmentDetail {
  const status = pick(SHIPMENT_STATUSES, index);
  const no = String(index + 1).padStart(4, '0');
  const dispatch = weighing('2000.000', '17.500', 40);
  return {
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
    createdAt: new Date(Date.UTC(2026, 9, 20, 6) - index * 3_600_000).toISOString(),
    allowedActions: status === 'DRAFT' ? ['ASSIGN_BAGS', 'RECORD_DISPATCH_WEIGHING', 'CANCEL'] : [],
    ...(DISPATCHED.has(status) && status !== 'CANCELLED'
      ? { sealNo: `M-${no}`, dispatchWeighing: dispatch }
      : {}),
    ...(DELIVERED.has(status) ? { deliveryWeighing: weighing('1985.000', '17.000', 40) } : {}),
  };
}

export const MOCK_SHIPMENTS: readonly ShipmentDetail[] = Array.from(
  { length: SHIPMENT_COUNT },
  (_, index) => createShipment(index),
);
