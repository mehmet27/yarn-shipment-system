import type { Kilogram } from '@iplik/units';
import { isKilogram } from '@iplik/units';
import type {
  CompleteShipmentRequest,
  DispatchWeighingRequest as GeneratedDispatchWeighingRequest,
  InternalShipmentView as GeneratedInternalView,
  ListShipmentsParams,
  ShipmentAction,
  ShipmentDetailResponse,
  ShipmentPageResponse,
  ShipmentStatus,
  ShipmentSummaryView,
  WarehouseShipmentView as GeneratedWarehouseView,
  WeighingView,
} from './generated/shipmentService';
import {
  completeShipment,
  getShipment as getGeneratedShipment,
  listShipments,
  recordDispatchWeighing,
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
  WithWeighings<GeneratedInternalView, 'dispatchWeighing'>,
  'plannedNetKg'
> & { plannedNetKg?: Kilogram };
export type WarehouseShipmentView = Omit<
  WithWeighings<GeneratedWarehouseView, 'dispatchWeighing'>,
  'plannedNetKg'
> & { plannedNetKg?: Kilogram };
export type ShipmentDetail = InternalShipmentView | WarehouseShipmentView;

export type ShipmentSummary = ShipmentSummaryView;
export type Page<T> = { items: readonly T[]; page: ShipmentPageResponse['page'] };
export type PageInfo = ShipmentPageResponse['page'];
export type { ListShipmentsParams, ShipmentAction, ShipmentStatus };

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
        isValidOptionalWeighing(shipment.dispatchWeighing)
      );
    case 'WAREHOUSE':
      return (
        isValidOptionalKg(shipment.plannedNetKg) &&
        isValidOptionalWeighing(shipment.dispatchWeighing)
      );
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

export type DispatchWeighingRequest = Omit<
  GeneratedDispatchWeighingRequest,
  'grossKg' | 'tareKg'
> & {
  grossKg: Kilogram;
  tareKg: Kilogram;
};

export type { CompleteShipmentRequest };

export { completeShipment, listShipments, recordDispatchWeighing };
