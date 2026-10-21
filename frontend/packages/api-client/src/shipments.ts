import type { Kilogram } from '@iplik/units';
import { isKilogram } from '@iplik/units';
import type {
  DriverShipmentView as GeneratedDriverView,
  InternalShipmentView as GeneratedInternalView,
  ListShipmentsParams,
  SealCondition,
  ShipmentAction,
  ShipmentDetailResponse,
  ShipmentPageResponse,
  ShipmentStatus,
  ShipmentSummaryView,
  SubcontractorShipmentView as GeneratedSubcontractorView,
  WarehouseShipmentView as GeneratedWarehouseView,
  WeighingView,
} from './generated/shipmentService';
import {
  confirmPickup,
  getShipment as getGeneratedShipment,
  listShipments,
  ShipmentStatus as ShipmentStatusValues,
} from './generated/shipmentService';

/**
 * Üretilmiş sözleşme tipleri (FE-API-01). Tek fark: kilo alanları doğrulanmış `Kilogram` tipine
 * daraltılır, böylece `@iplik/units` dışında aritmetik yapılamaz (FE-NUM-01).
 */
export type Weighing = Omit<WeighingView, 'grossKg' | 'tareKg' | 'netKg'> & {
  grossKg: Kilogram;
  tareKg: Kilogram;
  netKg: Kilogram;
};

type WithWeighings<T, K extends keyof T> = Omit<T, K> & Partial<Record<K, Weighing>>;

export type InternalShipmentView = Omit<
  WithWeighings<GeneratedInternalView, 'dispatchWeighing' | 'deliveryWeighing'>,
  'plannedNetKg'
> & { plannedNetKg?: Kilogram };
export type WarehouseShipmentView = Omit<
  WithWeighings<GeneratedWarehouseView, 'dispatchWeighing'>,
  'plannedNetKg'
> & { plannedNetKg?: Kilogram };
export type SubcontractorShipmentView = WithWeighings<
  GeneratedSubcontractorView,
  'deliveryWeighing'
>;
export type DriverShipmentView = GeneratedDriverView;
export type ShipmentDetail =
  InternalShipmentView | WarehouseShipmentView | SubcontractorShipmentView | DriverShipmentView;

export type ShipmentSummary = ShipmentSummaryView;
export type Page<T> = { items: readonly T[]; page: ShipmentPageResponse['page'] };
export type PageInfo = ShipmentPageResponse['page'];
export type { ListShipmentsParams, SealCondition, ShipmentAction, ShipmentStatus };

export const SHIPMENTS_PATH = '/api/v1/shipments';
export const SHIPMENT_STATUSES: readonly ShipmentStatus[] = Object.values(ShipmentStatusValues);

function isWeighing(weighing: WeighingView): weighing is Weighing {
  return isKilogram(weighing.grossKg) && isKilogram(weighing.tareKg) && isKilogram(weighing.netKg);
}

function isValidOptionalKg(value: string | undefined): boolean {
  return value === undefined || isKilogram(value);
}

function isValidOptionalWeighing(weighing: WeighingView | undefined): boolean {
  return weighing === undefined || isWeighing(weighing);
}

function hasValidKilograms(shipment: ShipmentDetailResponse): shipment is ShipmentDetail {
  switch (shipment.audience) {
    case 'INTERNAL':
      return (
        isValidOptionalKg(shipment.plannedNetKg) &&
        isValidOptionalWeighing(shipment.dispatchWeighing) &&
        isValidOptionalWeighing(shipment.deliveryWeighing)
      );
    case 'WAREHOUSE':
      return (
        isValidOptionalKg(shipment.plannedNetKg) &&
        isValidOptionalWeighing(shipment.dispatchWeighing)
      );
    case 'SUBCONTRACTOR':
      return isValidOptionalWeighing(shipment.deliveryWeighing);
    case 'DRIVER':
      return true;
  }
}

/** Sözleşme dışı kilo biçimi gelirse ekrana yanlış sayı basmak yerine hata verilir. */
export async function getShipment(shipmentId: string): Promise<ShipmentDetail> {
  const shipment = await getGeneratedShipment(encodeURIComponent(shipmentId));
  if (!hasValidKilograms(shipment)) {
    throw new TypeError(`Invalid kilogram format shipmentId=${shipmentId}`);
  }
  return shipment;
}

export { confirmPickup, listShipments };
