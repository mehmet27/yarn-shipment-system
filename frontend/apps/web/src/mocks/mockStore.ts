import type {
  CreateShipmentRequest,
  WeightCheckView,
  InternalShipmentView,
  ShipmentStatus,
  Weighing,
} from '@iplik/api-client';
import { isKilogram, sumKg } from '@iplik/units';
import { resetMockCorrections } from './corrections';
import { resetMockClaims } from './claims';
import { resetMockLoginAccounts } from './loginAccounts';
import { resetMockLots } from './lots';
import { resetMockSettlements } from './settlements';
import { resetMasterData } from './masterData';
import { allowedActionsOf, MOCK_SHIPMENTS } from './shipments';

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

/** Çıkış tartısı: beklenenle tutarsa AWAITING_DELIVERY, tutmazsa WEIGHT_MISMATCH. */
export function recordMockDispatch(
  shipmentId: string,
  dispatchWeighing: Weighing,
  weightCheck?: WeightCheckView,
): void {
  const status: ShipmentStatus =
    weightCheck === undefined ? 'AWAITING_DELIVERY' : 'WEIGHT_MISMATCH';
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId
      ? {
          ...shipment,
          status,
          allowedActions: allowedActionsOf(status),
          dispatchWeighing,
          ...(weightCheck === undefined ? {} : { weightCheck }),
        }
      : shipment,
  );
}

const DECISION_STATUS: Readonly<Record<string, ShipmentStatus>> = {
  CANCEL: 'CANCELLED',
  ADD_REMOVE_BAGS: 'DRAFT',
  UPDATE_RECIPE: 'AWAITING_DELIVERY',
};

/** Müdür kararı: çuval ekle/çıkar taslağa döner; reçete güncellenirse sevkiyat yola çıkar. */
export function decideMockWeight(shipmentId: string, decision: string): void {
  const status = DECISION_STATUS[decision] ?? 'DRAFT';
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId
      ? { ...shipment, status, allowedActions: allowedActionsOf(status) }
      : shipment,
  );
}

/** İrsaliyeyle tamamlama: AWAITING_DELIVERY → COMPLETED; irsaliye numarası sevkiyata yazılır. */
export function completeMockShipment(shipmentId: string, waybillNo: string): void {
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId
      ? {
          ...shipment,
          status: 'COMPLETED',
          allowedActions: allowedActionsOf('COMPLETED'),
          waybillNo,
        }
      : shipment,
  );
}

/** Admin düzeltmesi: çıkış tartısı yeni değeriyle değişir (eskisi düzeltme kaydında kalır). */
export function patchMockShipment(
  shipmentId: string,
  patch: Pick<Partial<InternalShipmentView>, 'dispatchWeighing'>,
): void {
  shipments = shipments.map((shipment) =>
    shipment.shipmentId === shipmentId ? { ...shipment, ...patch } : shipment,
  );
}

/** Sevkiyat oluşturma: yeni kayıt DRAFT durumunda ve en başa eklenir. */
export function addMockShipment(request: CreateShipmentRequest): InternalShipmentView {
  const no = String(shipments.length + 1).padStart(4, '0');
  const planned = sumKg(request.lines.map((line) => line.netKg));
  const created: InternalShipmentView = {
    audience: 'INTERNAL',
    shipmentId: `shp-${no}`,
    shipmentNo: `SVK-2026-${no}`,
    siteId: request.siteId,
    status: 'DRAFT',
    subcontractorId: request.subcontractorId,
    subcontractorTitle: request.subcontractorTitle,
    ...(request.sourceSubcontractorTitle === undefined
      ? {}
      : { sourceSubcontractorTitle: request.sourceSubcontractorTitle }),
    ...(request.driverName === undefined ? {} : { driverName: request.driverName }),
    ...(request.plateNo === undefined ? {} : { plateNo: request.plateNo }),
    createdAt: new Date().toISOString(),
    allowedActions: ['ASSIGN_BAGS', 'RECORD_DISPATCH', 'CANCEL'],
    lines: request.lines.map(({ yarnTypeId, yarnTypeText, lotId, netKg }, index) => ({
      lineNo: index + 1,
      netKg,
      ...(yarnTypeId === undefined ? {} : { yarnTypeId }),
      ...(yarnTypeText === undefined ? {} : { yarnTypeText }),
      ...(lotId === undefined ? {} : { lotId }),
    })),
    ...(request.fireRate === undefined ? {} : { fireRate: request.fireRate }),
    ...(isKilogram(planned) ? { plannedNetKg: planned } : {}),
  };
  shipments = [created, ...shipments];
  return created;
}

/** Sevkiyata eklenen çuval kiloları. */
let assignedBagWeights = new Map<string, string[]>();

/** Eklenen çuval sayısını döner. */
export function assignMockBagWeights(shipmentId: string, weights: readonly string[]): number {
  assignedBagWeights.set(shipmentId, [...(assignedBagWeights.get(shipmentId) ?? []), ...weights]);
  return weights.length;
}

export function resetMockStore(): void {
  resetMasterData();
  resetMockCorrections();
  resetMockClaims();
  resetMockLots();
  resetMockSettlements();
  resetMockLoginAccounts();
  shipments = [...MOCK_SHIPMENTS];
  assignedBagWeights = new Map();
}
