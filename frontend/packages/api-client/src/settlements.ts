import { customInstance } from './customInstance';
import type { Page } from './shipments';

/**
 * Bakiye, kumaş girişi, iplik dönüşü ve fire dosyası uçları (analiz Bölüm 5.7-5.11). Kilolar ve oranlar
 * sunucudan dize gelir (3 ondalık); aritmetik yalnızca `@iplik/units` içinde yapılır.
 */
export const BALANCES_PATH = '/api/v1/balances';
export const FIRE_SETTLEMENTS_PATH = '/api/v1/fire-settlements';
export const FIRE_INVOICES_PATH = '/api/v1/fire-invoices';

export type BalanceRow = {
  subcontractorId: string;
  subcontractorTitle: string;
  yarnType: string;
  balanceKg: string;
  inTransitKg: string;
};
export type BalancesView = { items: BalanceRow[]; totalKg: string };

export type MovementKind =
  | 'DISPATCH'
  | 'FABRIC_RECEIPT'
  | 'YARN_RETURN'
  | 'YARN_FROM_SUBCONTRACTOR'
  | 'CLAIM_ADJUSTMENT'
  | 'INVOICE'
  | 'OPENING'
  | 'CORRECTION';

export type BalanceMovement = {
  id: string;
  subcontractorId: string;
  yarnType: string;
  lotId?: string;
  shipmentId?: string;
  kind: MovementKind;
  netKg: string;
  occurredAt: string;
  createdBy: string;
  reason?: string;
};
/** Hareket sayfası; `actorNames` girenlerin okunur adları, çözülemeyen kişi yoktur. */
export type MovementPage = Page<BalanceMovement> & {
  actorNames?: Readonly<Record<string, string>>;
};
export type ListMovementsParams = {
  subcontractorId?: string;
  yarnType?: string;
  page?: number;
  size?: number;
};

export type YarnShare = { yarnType: string; netKg: string };
export type FabricReceipt = {
  id: string;
  fabricType: string;
  netKg: string;
  rollCount?: number;
  waybillNo: string;
  receivedAt: string;
  yarns: YarnShare[];
};
export type FabricReceiptRequest = {
  fabricType: string;
  netKg: string;
  rollCount?: number;
  waybillNo: string;
  receivedAt?: string;
  yarns?: YarnShare[];
};

/** FROM_SUBCONTRACTOR artık yeni kayıt açılmaz; yalnızca eski kayıtlar listede görünür. */
export type YarnReturnDirection = 'RETURN' | 'FROM_SUBCONTRACTOR';
export type YarnReturn = {
  id: string;
  direction: YarnReturnDirection;
  yarnType: string;
  lotId?: string;
  netKg: string;
  waybillNo?: string;
  createdAt: string;
};
export type YarnReturnRequest = {
  yarnType: string;
  lotId?: string;
  netKg: string;
  waybillNo?: string;
};

export type FireStatus = 'AWAITING_FABRIC' | 'TO_INVOICE' | 'INVOICED' | 'CLOSED';
export const FIRE_STATUSES: readonly FireStatus[] = [
  'AWAITING_FABRIC',
  'TO_INVOICE',
  'INVOICED',
  'CLOSED',
];
export type SettlementYarn = { yarnType: string; dispatchedKg: string };
export type InvoiceEntry = { invoiceNo: string; invoicedOn: string; kg: string };
export type FireSettlement = {
  shipmentId: string;
  status: FireStatus;
  fireRate: string;
  sentKg: string;
  returnedKg: string;
  claimKg: string;
  fabricKg: string;
  baseKg: string;
  expectedFabricKg: string;
  shortageKg: string;
  faultyKg: string;
  actualFirePercent: string;
  /** Şimdiye kadar faturası kesilen kilo. */
  invoicedKg: string;
  /** Henüz faturası kesilmemiş kilo (faturalanacak kilo): eksik kumaş ve haklı itirazdan kalan. */
  remainingKg: string;
  /** remainingKg'nin haklı kalite itirazından gelen kısmı; fatura kesilince fasoncu bakiyesinden düşer. */
  claimRemainingKg: string;
  /** Faturalanan kilonun eksik kumaşı karşılayan kısmı; kumaş girişi sınırında tüketilmiş sayılır. */
  shortageInvoicedKg: string;
  /** Sevkiyatta giden iplikler; fatura satırlarında kilo bu ipliklere yazılır. */
  yarns: SettlementYarn[];
  /** Şimdiye kadar girilen faturalar. */
  invoices: InvoiceEntry[];
  invoiceNo?: string;
  invoicedAt?: string;
  invoicedBy?: string;
  invoiceNote?: string;
};
export type FireRateRequest = { fireRate: string; reason: string };
/** `lines` boşsa kalan kilonun tamamı iplik oranıyla faturalanır. */
export type InvoiceRequest = {
  invoiceNo: string;
  invoicedOn: string;
  note?: string;
  lines?: YarnShare[];
};

export type FireSettlementRow = {
  shipmentId: string;
  shipmentNo: string;
  subcontractorTitle: string;
  status: FireStatus;
  sentKg: string;
  fabricKg: string;
  expectedFabricKg: string;
  faultyKg: string;
  actualFirePercent: string;
  invoicedKg: string;
  remainingKg: string;
};
export type ListFireSettlementsParams = {
  status?: FireStatus;
  subcontractorId?: string;
  page?: number;
  size?: number;
};

/** Faturalananlar listesinde bir fatura: hangi sevkiyata, hangi fasoncuya ve kaç kilo girildiği. */
export type FireInvoiceRow = {
  invoiceNo: string;
  /** Fatura tarihi, `YYYY-MM-DD`. */
  invoicedOn: string;
  shipmentId: string;
  shipmentNo: string;
  subcontractorId: string;
  subcontractorTitle: string;
  kg: string;
};
/** `totalKg` yalnız bu sayfanın değil, süzgece uyan tüm faturaların toplamıdır. */
export type FireInvoicePage = Page<FireInvoiceRow> & { totalKg: string };
export type ListFireInvoicesParams = {
  from?: string;
  to?: string;
  subcontractorId?: string;
  page?: number;
  size?: number;
};

export const getBalances = (subcontractorId?: string) =>
  customInstance<BalancesView>({
    url: BALANCES_PATH,
    method: 'GET',
    params: subcontractorId === undefined ? undefined : { subcontractorId },
  });

export const listBalanceMovements = (params: ListMovementsParams) =>
  customInstance<MovementPage>({
    url: `${BALANCES_PATH}/movements`,
    method: 'GET',
    params,
  });

const shipmentUrl = (shipmentId: string, path: string) => `/api/v1/shipments/${shipmentId}/${path}`;

export const listFabricReceipts = (shipmentId: string) =>
  customInstance<FabricReceipt[]>({
    url: shipmentUrl(shipmentId, 'fabric-receipts'),
    method: 'GET',
  });
export const recordFabricReceipt = (shipmentId: string, data: FabricReceiptRequest) =>
  customInstance<FabricReceipt>({
    url: shipmentUrl(shipmentId, 'fabric-receipts'),
    method: 'POST',
    data,
  });

export const listYarnReturns = (shipmentId: string) =>
  customInstance<YarnReturn[]>({ url: shipmentUrl(shipmentId, 'yarn-returns'), method: 'GET' });
export const recordYarnReturn = (shipmentId: string, data: YarnReturnRequest) =>
  customInstance<YarnReturn>({
    url: shipmentUrl(shipmentId, 'yarn-returns'),
    method: 'POST',
    data,
  });

export const getFireSettlement = (shipmentId: string) =>
  customInstance<FireSettlement>({
    url: shipmentUrl(shipmentId, 'fire-settlement'),
    method: 'GET',
  });
export const updateFireRate = (shipmentId: string, data: FireRateRequest) =>
  customInstance<FireSettlement>({
    url: shipmentUrl(shipmentId, 'fire-rate'),
    method: 'PATCH',
    data,
  });
export const markInvoiced = (shipmentId: string, data: InvoiceRequest) =>
  customInstance<FireSettlement>({
    url: shipmentUrl(shipmentId, 'fire-settlement/invoice'),
    method: 'POST',
    data,
  });
export const closeFireSettlement = (shipmentId: string) =>
  customInstance<FireSettlement>({
    url: shipmentUrl(shipmentId, 'fire-settlement/close'),
    method: 'POST',
  });

export const listFireSettlements = (params: ListFireSettlementsParams) =>
  customInstance<Page<FireSettlementRow>>({ url: FIRE_SETTLEMENTS_PATH, method: 'GET', params });

export const listFireInvoices = (params: ListFireInvoicesParams) =>
  customInstance<FireInvoicePage>({ url: FIRE_INVOICES_PATH, method: 'GET', params });
