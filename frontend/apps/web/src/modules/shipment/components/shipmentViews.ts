import type { ShipmentDetail, Weighing } from '@iplik/api-client';

export type ShipmentWeighings = {
  dispatchWeighing?: Weighing;
  deliveryWeighing?: Weighing;
  /** Bu görünümde hiç gelmeyen taraflar: değer yok demek "kaydedilmedi" değil, "göremezsiniz"dir. */
  hidden: { dispatch: boolean; delivery: boolean };
};

type Audience = ShipmentDetail['audience'];

const HIDDEN_SIDES: Readonly<Record<Audience, ShipmentWeighings['hidden']>> = {
  INTERNAL: { dispatch: false, delivery: false },
  WAREHOUSE: { dispatch: false, delivery: true },
  SUBCONTRACTOR: { dispatch: true, delivery: false },
  DRIVER: { dispatch: true, delivery: true },
};

/** Görünüm türüne göre var olan tartım alanları; kör alanlar görünümde hiç yoktur. */
export function toWeighings(shipment: ShipmentDetail): ShipmentWeighings {
  return { ...pickWeighings(shipment), hidden: HIDDEN_SIDES[shipment.audience] };
}

function pickWeighings(shipment: ShipmentDetail): Omit<ShipmentWeighings, 'hidden'> {
  switch (shipment.audience) {
    case 'INTERNAL':
      return pickDefined({
        dispatchWeighing: shipment.dispatchWeighing,
        deliveryWeighing: shipment.deliveryWeighing,
      });
    case 'WAREHOUSE':
      return pickDefined({ dispatchWeighing: shipment.dispatchWeighing });
    case 'SUBCONTRACTOR':
      return pickDefined({ deliveryWeighing: shipment.deliveryWeighing });
    case 'DRIVER':
      return {};
  }
}

/** Görünümde olmayan alan için gösterilecek metnin i18n anahtarı. */
export type MissingKey = 'common.notRecorded' | 'common.hiddenForRole' | 'common.blindCount';

/** Değer görünümde yoksa: alan bu rolün görünümünde hiç yoksa gizlidir, varsa henüz kaydedilmemiştir. */
export type FieldValue = { value: string } | { missing: MissingKey };

function present(value: string | number | undefined): FieldValue {
  return value === undefined ? { missing: 'common.notRecorded' } : { value: String(value) };
}

/**
 * Görünümde hiç olmayan çıkış alanları: fasoncu çuval sayısını ve mührü kör sayım gereği görmez
 * (FE-TEST-05); şoför görünümünde şoför adı yoktur (kendisidir).
 */
const ABSENT_FIELDS: Readonly<Record<Audience, Partial<Record<string, MissingKey>>>> = {
  INTERNAL: {},
  WAREHOUSE: {},
  SUBCONTRACTOR: { sealNo: 'common.blindCount', bagCount: 'common.blindCount' },
  DRIVER: { driverName: 'common.hiddenForRole' },
};

function valueOf(
  shipment: ShipmentDetail,
  field: 'sealNo' | 'bagCount' | 'driverName',
  value: string | number | undefined,
): FieldValue {
  const missing = ABSENT_FIELDS[shipment.audience][field];
  return missing === undefined ? present(value) : { missing };
}

export function findSealNo(shipment: ShipmentDetail): FieldValue {
  return valueOf(
    shipment,
    'sealNo',
    'dispatchSealNo' in shipment ? shipment.dispatchSealNo : undefined,
  );
}

export function findBagCount(shipment: ShipmentDetail): FieldValue {
  const count = 'dispatchBagCount' in shipment ? shipment.dispatchBagCount : undefined;
  return valueOf(shipment, 'bagCount', count);
}

export function findDriverName(shipment: ShipmentDetail): FieldValue {
  return valueOf(
    shipment,
    'driverName',
    'driverName' in shipment ? shipment.driverName : undefined,
  );
}

/** exactOptionalPropertyTypes: undefined değerli alanlar nesneye yazılmaz. */
function pickDefined({
  dispatchWeighing,
  deliveryWeighing,
}: {
  dispatchWeighing?: Weighing | undefined;
  deliveryWeighing?: Weighing | undefined;
}): Omit<ShipmentWeighings, 'hidden'> {
  return {
    ...(dispatchWeighing === undefined ? {} : { dispatchWeighing }),
    ...(deliveryWeighing === undefined ? {} : { deliveryWeighing }),
  };
}

const DELIVERED: ReadonlySet<ShipmentDetail['status']> = new Set([
  'CHECKING',
  'IN_REVIEW',
  'COMPLETED',
]);

/** Kalite itirazı teslimden sonra konuşulur; asıl pencere ve kapsam kontrolü backend'dedir. */
export function isDelivered(shipment: ShipmentDetail): boolean {
  return DELIVERED.has(shipment.status);
}
