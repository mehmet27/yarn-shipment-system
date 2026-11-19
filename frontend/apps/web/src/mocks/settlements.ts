import type {
  BalanceMovement,
  FireInvoiceRow,
  BalanceRow,
  FabricReceipt,
  FabricReceiptRequest,
  FireSettlement,
  MovementKind,
  YarnReturn,
  YarnReturnRequest,
  YarnShare,
} from '@iplik/api-client/settlements';
import type { Kilogram } from '@iplik/units';
import { calculateFire, sumKg } from '@iplik/units';
import { listMockShipments } from './mockStore';

/** Sahte API'nin bakiye, kumaş, iplik dönüşü ve fire dosyası durumu; testler arasında sıfırlanır. */
type Invoice = { invoiceNo: string; invoicedAt: string; invoiceNote?: string; kg: string };

export const DEFAULT_YARN = '30/1 Penye';
const DEFAULT_RATE = '0.0400';
const ZERO = '0.000';

let receipts = new Map<string, FabricReceipt[]>();
let returns = new Map<string, YarnReturn[]>();
let rates = new Map<string, string>();
let invoices = new Map<string, Invoice[]>();
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
export const isMockClosed = (id: string) =>
  closed.has(id) || getMockSettlement(id).status === 'INVOICED';
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
    direction: 'RETURN',
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
      kind: 'YARN_RETURN',
      yarnType: request.yarnType,
      netKg: `-${request.netKg}`,
    },
    userId,
  );
  return entry;
}

const toKg = (value: number) => value.toFixed(3);

/** Faturayı yazar: iplik satırları yoksa kalan kilo tek iplikten faturalanır; her satır bakiyeden düşer. */
export function markMockInvoiced(
  shipmentId: string,
  input: Omit<Invoice, 'kg'> & { lines: readonly YarnShare[]; userId: string },
): void {
  const { lines, userId, ...invoice } = input;
  const shares =
    lines.length === 0
      ? [{ yarnType: DEFAULT_YARN, netKg: getMockSettlement(shipmentId).remainingKg }]
      : lines;
  const kg = toKg(shares.reduce((sum, line) => sum + Number(line.netKg), 0));
  invoices.set(shipmentId, [...(invoices.get(shipmentId) ?? []), { ...invoice, kg }]);
  for (const line of shares) {
    addMovement(
      { shipmentId, kind: 'INVOICE', yarnType: line.yarnType, netKg: `-${line.netKg}` },
      userId,
    );
  }
}
/** Girilmiş tüm faturalar, sevkiyat ve fasoncu bilgisiyle; yeniden eskiye (fatura tarihi, sonra giriş sırası). */
export function listMockInvoiceRows(): FireInvoiceRow[] {
  const shipments = listMockShipments();
  const rows: { row: FireInvoiceRow; order: number }[] = [];
  let order = 0;
  for (const [shipmentId, entries] of invoices) {
    const shipment = shipments.find((s) => s.shipmentId === shipmentId);
    if (shipment === undefined) continue;
    for (const entry of entries) {
      order += 1;
      rows.push({
        order,
        row: {
          invoiceNo: entry.invoiceNo,
          invoicedOn: entry.invoicedAt,
          shipmentId,
          shipmentNo: shipment.shipmentNo,
          subcontractorId: shipment.subcontractorId,
          subcontractorTitle: shipment.subcontractorTitle,
          kg: entry.kg,
        },
      });
    }
  }
  return rows
    .sort((a, b) => b.row.invoicedOn.localeCompare(a.row.invoicedOn) || b.order - a.order)
    .map(({ row }) => row);
}

export function closeMockFile(shipmentId: string): void {
  closed.add(shipmentId);
}

function statusOf(
  shipmentId: string,
  hasFabric: boolean,
  isFullyInvoiced: boolean,
): FireSettlement['status'] {
  if (isFullyInvoiced) return 'INVOICED';
  if (closed.has(shipmentId)) return 'CLOSED';
  return hasFabric ? 'TO_INVOICE' : 'AWAITING_FABRIC';
}

/** Faturalanan kilo kumaş gibi sayılır: fire = (T − K − faturalanan) / T. */
function firePercentAfterInvoice(
  baseKg: string,
  fabric: { netKg: string }[],
  invoicedKg: string,
): string {
  const base = Number(baseKg);
  if (base <= 0) return '0.00';
  const received = fabric.reduce((sum, r) => sum + Number(r.netKg), 0);
  return (((base - received - Number(invoicedKg)) / base) * 100).toFixed(2);
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
  const entries = invoices.get(shipmentId) ?? [];
  const invoicedKg = toKg(entries.reduce((sum, entry) => sum + Number(entry.kg), 0));
  const remaining = Math.max(0, Number(result.faultyKg) - Number(invoicedKg));
  const isFullyInvoiced = entries.length > 0 && remaining <= 0;
  const last = entries.at(-1);
  return {
    shipmentId,
    status: statusOf(shipmentId, fabric.length > 0, isFullyInvoiced),
    fireRate,
    sentKg,
    returnedKg,
    claimKg: ZERO,
    fabricKg,
    ...result,
    actualFirePercent: firePercentAfterInvoice(result.baseKg, fabric, invoicedKg),
    invoicedKg,
    remainingKg: toKg(remaining),
    claimRemainingKg: ZERO,
    shortageInvoicedKg: invoicedKg,
    yarns: [{ yarnType: DEFAULT_YARN, dispatchedKg: sentKg }],
    invoices: entries.map((e) => ({ invoiceNo: e.invoiceNo, invoicedOn: e.invoicedAt, kg: e.kg })),
    ...(isFullyInvoiced && last !== undefined
      ? {
          invoiceNo: last.invoiceNo,
          invoicedAt: last.invoicedAt,
          invoicedBy: 'user-manager',
          ...(last.invoiceNote === undefined ? {} : { invoiceNote: last.invoiceNote }),
        }
      : {}),
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
