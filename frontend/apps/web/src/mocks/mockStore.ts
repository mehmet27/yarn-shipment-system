import type { InternalShipmentView, ShipmentStatus, Weighing } from '@iplik/api-client';
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

export function resetMockStore(): void {
  shipments = [...MOCK_SHIPMENTS];
}
