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
  | 'RECORD_DISPATCH_WEIGHING'
  | 'CONFIRM_PICKUP'
  | 'SUBMIT_DELIVERY_RECEIPT'
  | 'CANCEL';

export type ShipmentSummary = {
  shipmentId: string;
  shipmentNo: string;
  siteId: string;
  status: ShipmentStatus;
  subcontractorId: string;
  subcontractorTitle: string;
  driverName: string;
  plateNo: string;
  plannedBagCount: number;
  plannedNetKg: Kilogram;
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

/** İç kullanıcı görünümü. Kör giriş: görme yetkisi yoksa tartım alanı yanıtta hiç yoktur. */
export type ShipmentDetail = ShipmentSummary & {
  sealNo?: string;
  dispatchWeighing?: Weighing;
  deliveryWeighing?: Weighing;
};

export type PageInfo = { number: number; size: number; totalElements: number; totalPages: number };
export type Page<T> = { items: readonly T[]; page: PageInfo };

export type ListShipmentsParams = {
  page: number;
  size: number;
  status?: ShipmentStatus;
};

export const SHIPMENTS_PATH = '/api/v1/shipments';

export async function listShipments(params: ListShipmentsParams): Promise<Page<ShipmentSummary>> {
  const response = await httpClient.get<Page<ShipmentSummary>>(SHIPMENTS_PATH, {
    params: { ...params, sort: 'createdAt,desc' },
  });
  return response.data;
}

export async function getShipment(shipmentId: string): Promise<ShipmentDetail> {
  const response = await httpClient.get<ShipmentDetail>(
    `${SHIPMENTS_PATH}/${encodeURIComponent(shipmentId)}`,
  );
  return response.data;
}
