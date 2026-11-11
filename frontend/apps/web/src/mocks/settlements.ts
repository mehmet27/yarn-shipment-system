import type {
  BalanceMovement,
  BalanceRow,
  FabricReceipt,
  FabricReceiptRequest,
  FireSettlement,
  MovementKind,
  YarnReturn,
  YarnReturnRequest,
} from '@iplik/api-client/settlements';
import type { Kilogram } from '@iplik/units';
import { calculateFire, sumKg } from '@iplik/units';
import { listMockShipments } from './mockStore';

/** Sahte API'nin bakiye, kumaş, iplik dönüşü ve fire dosyası durumu; testler arasında sıfırlanır. */
type Invoice = { invoiceNo: string; invoicedAt: string; invoiceNote?: string };

export const DEFAULT_YARN = '30/1 Penye';
const DEFAULT_RATE = '0.0400';
const ZERO = '0.000';

let receipts = new Map<string, FabricReceipt[]>();
let returns = new Map<string, YarnReturn[]>();
let rates = new Map<string, string>();
let invoices = new Map<string, Invoice>();
let closed = new Set<string>();
let movements: BalanceMovement[] = [];
let counter = 0;

export function resetMockSettlements(): void {
  receipts = new Map();
  returns = new Map();
  rates = new Map();
  invoices = new Map();
  closed = new Set();
  movements = [];
  counter = 0;
}

const nextId = (prefix: string) => `${prefix}-${String((counter += 1))}`;
const asKg = (value: string): Kilogram => value as Kilogram;

export const listMockReceipts = (id: string) => receipts.get(id) ?? [];
export const listMockReturns = (id: string) => returns.get(id) ?? [];
export const isMockClosed = (id: string) => closed.has(id) || invoices.has(id);
export const setMockRate = (id: string, rate: string) => rates.set(id, rate);

function sentKgOf(shipmentId: string): string {
  const net = listMockShipments().find((s) => s.shipmentId === shipmentId)?.dispatchWeighing?.netKg;
  return net ?? '34032.000';
}

type NewMovement = {
  shipmentId: string;
  kind: MovementKind;
  yarnType: string;
  netKg: string;
};

function addMovement(entry: NewMovement, createdBy: string): void {
  const shipment = listMockShipments().find((s) => s.shipmentId === entry.shipmentId);
  movements = [
    {
      id: nextId('mov'),
      subcontractorId: shipment?.subcontractorId ?? 'sub-0',
      occurredAt: new Date().toISOString(),
      createdBy,
      ...entry,
    },
    ...movements,
  ];
}

export function addMockReceipt(
  shipmentId: string,
  request: FabricReceiptRequest,
  userId: string,
): FabricReceipt {
  const yarns = request.yarns ?? [{ yarnType: DEFAULT_YARN, netKg: request.netKg }];
  const receipt: FabricReceipt = {
    id: nextId('fab'),
    fabricType: request.fabricType,
    netKg: request.netKg,
    ...(request.rollCount === undefined ? {} : { rollCount: request.rollCount }),
    waybillNo: request.waybillNo,
    receivedAt: request.receivedAt ?? new Date().toISOString(),
    yarns,
  };
  receipts.set(shipmentId, [...listMockReceipts(shipmentId), receipt]);
  for (const yarn of yarns) {
    addMovement(
      { shipmentId, kind: 'FABRIC_RECEIPT', yarnType: yarn.yarnType, netKg: `-${yarn.netKg}` },
      userId,
    );
  }
  return receipt;
}

export function addMockReturn(
  shipmentId: string,
  request: YarnReturnRequest,
  userId: string,
): YarnReturn {
  const entry: YarnReturn = {
    id: nextId('ret'),
    direction: request.direction,
    yarnType: request.yarnType,
    ...(request.lotId === undefined ? {} : { lotId: request.lotId }),
    netKg: request.netKg,
    ...(request.waybillNo === undefined ? {} : { waybillNo: request.waybillNo }),
    createdAt: new Date().toISOString(),
  };
  returns.set(shipmentId, [...listMockReturns(shipmentId), entry]);
  addMovement(
    {
      shipmentId,
      kind: request.direction === 'RETURN' ? 'YARN_RETURN' : 'YARN_FROM_SUBCONTRACTOR',
      yarnType: request.yarnType,
      netKg: `-${request.netKg}`,
    },
    userId,
  );
  return entry;
}

export function markMockInvoiced(shipmentId: string, invoice: Invoice): void {
  invoices.set(shipmentId, invoice);
}
export function closeMockFile(shipmentId: string): void {
  closed.add(shipmentId);
}

function statusOf(shipmentId: string, hasFabric: boolean): FireSettlement['status'] {
  if (invoices.has(shipmentId)) return 'INVOICED';
  if (closed.has(shipmentId)) return 'CLOSED';
  return hasFabric ? 'TO_INVOICE' : 'AWAITING_FABRIC';
}

export function getMockSettlement(shipmentId: string): FireSettlement {
  const returned = listMockReturns(shipmentId).filter((r) => r.direction === 'RETURN');
  const fabric = listMockReceipts(shipmentId);
  const fireRate = rates.get(shipmentId) ?? DEFAULT_RATE;
  const sentKg = sentKgOf(shipmentId);
  const returnedKg = sumKg(returned.map((r) => r.netKg));
  const fabricKg = sumKg(fabric.map((r) => r.netKg));
  const result = calculateFire({
    sentKg: asKg(sentKg),
    returnedKg: asKg(returnedKg),
    claimKg: asKg(ZERO),
    fabricKg: asKg(fabricKg),
    fireRate,
  });
  const invoice = invoices.get(shipmentId);
  return {
    shipmentId,
    status: statusOf(shipmentId, fabric.length > 0),
    fireRate,
    sentKg,
    returnedKg,
    claimKg: ZERO,
    fabricKg,
    ...result,
    ...(invoice === undefined
      ? {}
      : {
          invoiceNo: invoice.invoiceNo,
          invoicedAt: invoice.invoicedAt,
          invoicedBy: 'user-manager',
          ...(invoice.invoiceNote === undefined ? {} : { invoiceNote: invoice.invoiceNote }),
        }),
  };
}

export function listMockMovements(): readonly BalanceMovement[] {
  return movements;
}

type BalanceKey = { subcontractorId: string; subcontractorTitle: string; yarnType: string };

function titleOf(subcontractorId: string): string {
  return (
    listMockShipments().find((s) => s.subcontractorId === subcontractorId)?.subcontractorTitle ??
    subcontractorId
  );
}

function addToRow(rows: Map<string, BalanceRow>, key: BalanceKey, deltaKg: string): void {
  const id = `${key.subcontractorId}|${key.yarnType}`;
  rows.set(id, {
    ...key,
    balanceKg: sumKg([rows.get(id)?.balanceKg ?? ZERO, deltaKg]),
    inTransitKg: ZERO,
  });
}

/** Fasoncu x iplik cinsi bakiyesi: tamamlanan çıkışlar eksi kumaş/dönüş hareketleri. */
export function getMockBalanceRows(): BalanceRow[] {
  const rows = new Map<string, BalanceRow>();
  for (const shipment of listMockShipments().filter((s) => s.status === 'COMPLETED')) {
    const { subcontractorId, subcontractorTitle } = shipment;
    const key = { subcontractorId, subcontractorTitle, yarnType: DEFAULT_YARN };
    addToRow(rows, key, shipment.dispatchWeighing?.netKg ?? ZERO);
  }
  for (const move of movements) {
    const key = {
      subcontractorId: move.subcontractorId,
      subcontractorTitle: titleOf(move.subcontractorId),
      yarnType: move.yarnType,
    };
    addToRow(rows, key, move.netKg);
  }
  return [...rows.values()];
}
