import type {
  CreateShipmentRequest,
  InternalShipmentView,
  ShipmentStatus,
  Weighing,
} from '@iplik/api-client';
import { resetMasterData } from './masterData';
import { MOCK_SHIPMENTS } from './shipments';

/** Sahte API'nin değişebilir durumu; testler arasında resetMockStore ile sıfırlanır. */
let shipments: InternalShipmentView[] = [...MOCK_SHIPMENTS];

export function listMockShipments(): readonly InternalShipmentView[] {
  return shipments;
}

export function findMockShipment(shipmentId: unknown): InternalShipmentView | undefined {
  return shipments.find((shipment) => shipment.shipmentId === shipmentId);
}

export function updateMockShipmentStatus(shipmentId: string, status: ShipmentStatus): void {
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId ? { ...shipment, status, allowedActions: [] } : shipment,
  );
}

/** Çıkış tartısı: DRAFT → LOADED; tartı ve mühür sevkiyata yazılır. */
export function recordMockDispatch(
  shipmentId: string,
  dispatchWeighing: Weighing,
  dispatchSealNo: string,
): void {
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId
      ? { ...shipment, status: 'LOADED', allowedActions: [], dispatchWeighing, dispatchSealNo }
      : shipment,
  );
}

/** Mühür numarası tek kullanımlıktır (başka sevkiyatın çıkışında kullanılmış olamaz). */
export function isMockSealUsed(sealNo: string, exceptShipmentId: string): boolean {
  return shipments.some(
    (shipment) => shipment.dispatchSealNo === sealNo && shipment.shipmentId !== exceptShipmentId,
  );
}

/** Sevkiyat oluşturma: yeni kayıt DRAFT durumunda ve en başa eklenir. */
export function addMockShipment(request: CreateShipmentRequest): InternalShipmentView {
  const no = String(shipments.length + 1).padStart(4, '0');
  const created: InternalShipmentView = {
    audience: 'INTERNAL',
    randomCheckSelected: false,
    shipmentId: `shp-${no}`,
    shipmentNo: `SVK-2026-${no}`,
    siteId: request.siteId,
    status: 'DRAFT',
    subcontractorId: request.subcontractorId,
    subcontractorTitle: request.subcontractorTitle,
    ...(request.driverName === undefined ? {} : { driverName: request.driverName }),
    ...(request.plateNo === undefined ? {} : { plateNo: request.plateNo }),
    createdAt: new Date().toISOString(),
    allowedActions: ['ASSIGN_BAGS', 'RECORD_DISPATCH', 'CANCEL'],
  };
  shipments = [created, ...shipments];
  return created;
}

/** Sevkiyata atanmış çuval anahtarları; aynı anahtar ikinci kez atanınca sayılmaz (backend gibi). */
let assignedBagKeys = new Map<string, Set<string>>();

/** Yeni atanan anahtar sayısını döner. */
export function assignMockBagKeys(shipmentId: string, keys: readonly string[]): number {
  const assigned = assignedBagKeys.get(shipmentId) ?? new Set<string>();
  const fresh = keys.filter((key) => !assigned.has(key));
  for (const key of fresh) assigned.add(key);
  assignedBagKeys.set(shipmentId, assigned);
  return fresh.length;
}

export function resetMockStore(): void {
  resetMasterData();
  shipments = [...MOCK_SHIPMENTS];
  assignedBagKeys = new Map();
}
