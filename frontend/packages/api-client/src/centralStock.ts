import { customInstance } from './customInstance';

/** Merkez iplik stoku uçları: stok, merkeze iplik girişi ve merkez fabrika kumaş girişi. Kilolar dize gelir. */
export const CENTRAL_STOCK_PATH = '/api/v1/central-stock';

export type CentralStockKind = 'YARN_RECEIPT' | 'TO_SUBCONTRACTOR' | 'PRODUCTION';
export type CentralStockRow = { yarnType: string; stockKg: string };
export type CentralStockMovement = {
  id: string;
  yarnType: string;
  kind: CentralStockKind;
  netKg: string;
  note?: string;
  occurredAt: string;
};
export type CentralStock = {
  rows: CentralStockRow[];
  totalKg: string;
  /** Merkez fabrikada bugüne kadar üretilen toplam kumaş. */
  producedFabricKg: string;
  movements: CentralStockMovement[];
};

export type YarnReceiptRequest = { yarnType: string; netKg: string; note?: string };
export type CentralProductionRequest = {
  fabricType: string;
  /** Oran olarak, ör. "0.04". */
  fireRate: string;
  rollCount?: number;
  waybillNo?: string;
  yarns: { yarnType: string; netKg: string }[];
};
export type CentralProduction = {
  id: string;
  fabricType: string;
  netKg: string;
  fireRate: string;
  consumptions: CentralStockMovement[];
};

export const getCentralStock = () =>
  customInstance<CentralStock>({ url: CENTRAL_STOCK_PATH, method: 'GET' });
export const recordYarnReceipt = (data: YarnReceiptRequest) =>
  customInstance<CentralStockMovement>({
    url: `${CENTRAL_STOCK_PATH}/yarn-receipts`,
    method: 'POST',
    data,
  });
export const recordCentralProduction = (data: CentralProductionRequest) =>
  customInstance<CentralProduction>({
    url: `${CENTRAL_STOCK_PATH}/productions`,
    method: 'POST',
    data,
  });
