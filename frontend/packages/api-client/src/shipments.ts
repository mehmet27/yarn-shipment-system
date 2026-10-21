import type { Kilogram } from '@iplik/units';
import { httpClient } from './httpClient';

/**
 * Geçici sözleşme: shipment-service OpenAPI dosyası yayınlanınca bu dosya Orval çıktısıyla
 * değiştirilir (FE-API-01). Durumlar veritabanı CHECK değerleriyle birebir aynıdır.
 */
export type ShipmentStatus =
  | 'DRAFT'
  | 'LOADED'
  | 'IN_TRANSIT'
  | 'AWAITING_DELIVERY'
  | 'CHECKING'
  | 'IN_REVIEW'
  | 'COMPLETED'
  | 'CANCELLED';

export const SHIPMENT_STATUSES: readonly ShipmentStatus[] = [
  'DRAFT',
  'LOADED',
  'IN_TRANSIT',
  'AWAITING_DELIVERY',
  'CHECKING',
  'IN_REVIEW',
  'COMPLETED',
  'CANCELLED',
];

export type ShipmentAction =
  | 'ASSIGN_BAGS'
  | 'RECORD_DISPATCH'
  | 'CANCEL'
  | 'CONFIRM_PICKUP'
  | 'DECLARE_ARRIVAL'
  | 'RECORD_DELIVERY'
  | 'OPEN_QUALITY_CLAIM'
  | 'CREATE_CORRECTION';

/** Liste satırı her rol için aynıdır; beklenen yük kör giriş nedeniyle listede yoktur. */
export type ShipmentSummary = {
  shipmentId: string;
  shipmentNo: string;
  siteId: string;
  status: ShipmentStatus;
  subcontractorId: string;
  subcontractorTitle: string;
  driverName?: string;
  plateNo?: string;
  createdAt: string;
  allowedActions: readonly ShipmentAction[];
};

export type Weighing = {
  grossKg: Kilogram;
  tareKg: Kilogram;
  netKg: Kilogram;
  bagCount: number;
  weighedAt: string;
};

export type SealCondition = 'INTACT' | 'DAMAGED' | 'MISSING';

type ShipmentViewBase = {
  shipmentId: string;
  shipmentNo: string;
  siteId: string;
  status: ShipmentStatus;
  subcontractorTitle: string;
  plateNo?: string;
  createdAt: string;
  allowedActions: readonly ShipmentAction[];
};

export type InternalShipmentView = ShipmentViewBase & {
  audience: 'INTERNAL';
  subcontractorId: string;
  driverName?: string;
  plannedBagCount?: number;
  plannedNetKg?: Kilogram;
  dispatchWeighing?: Weighing;
  dispatchSealNo?: string;
  deliveryWeighing?: Weighing;
  deliverySealCondition?: SealCondition;
  randomCheckSelected: boolean;
  lockedAt?: string;
};

/** Depo teslim alanlarını görmez. */
export type WarehouseShipmentView = ShipmentViewBase & {
  audience: 'WAREHOUSE';
  subcontractorId: string;
  driverName?: string;
  plannedBagCount?: number;
  plannedNetKg?: Kilogram;
  dispatchWeighing?: Weighing;
  dispatchSealNo?: string;
  randomCheckSelected: boolean;
};

/** Kör giriş: fasoncu çıkış alanlarını ve beklenen yükü hiç almaz. */
export type SubcontractorShipmentView = ShipmentViewBase & {
  audience: 'SUBCONTRACTOR';
  driverName?: string;
  deliveryWeighing?: Weighing;
  deliverySealCondition?: SealCondition;
};

/** Şoför kilo görmez; yalnızca çuval sayısı ve mühür no. */
export type DriverShipmentView = ShipmentViewBase & {
  audience: 'DRIVER';
  dispatchBagCount?: number;
  dispatchSealNo?: string;
};

/** getShipment yanıtı: `audience` ile ayrılan oneOf (shipment-service OpenAPI). */
export type ShipmentDetail =
  InternalShipmentView | WarehouseShipmentView | SubcontractorShipmentView | DriverShipmentView;

export type PageInfo = { number: number; size: number; totalElements: number; totalPages: number };
export type Page<T> = { items: readonly T[]; page: PageInfo };

export type ListShipmentsParams = {
  page: number;
  size: number;
  status?: readonly ShipmentStatus[];
};

export const SHIPMENTS_PATH = '/api/v1/shipments';

export async function listShipments(params: ListShipmentsParams): Promise<Page<ShipmentSummary>> {
  // status dizi olarak tekrarlanan parametredir (?status=A&status=B, OpenAPI explode: true).
  const response = await httpClient.get<Page<ShipmentSummary>>(SHIPMENTS_PATH, {
    params,
    paramsSerializer: { indexes: null },
  });
  return response.data;
}

export async function getShipment(shipmentId: string): Promise<ShipmentDetail> {
  const response = await httpClient.get<ShipmentDetail>(
    `${SHIPMENTS_PATH}/${encodeURIComponent(shipmentId)}`,
  );
  return response.data;
}
