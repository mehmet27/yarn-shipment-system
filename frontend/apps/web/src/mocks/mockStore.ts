import type { InternalShipmentView, ShipmentStatus } from '@iplik/api-client';
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

export function resetMockStore(): void {
  shipments = [...MOCK_SHIPMENTS];
}
