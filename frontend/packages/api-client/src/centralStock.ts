import { customInstance } from './customInstance';

/** Merkez iplik stoku uçları: stok, merkeze iplik girişi ve merkez fabrika kumaş girişi. Kilolar dize gelir. */
export const CENTRAL_STOCK_PATH = '/api/v1/central-stock';

export type CentralStockKind = 'YARN_RECEIPT' | 'TO_SUBCONTRACTOR' | 'PRODUCTION';
/** `productId` boştur: Tanımlar'daki bir iplikle eşlenemeyen eski serbest metin satırı. */
export type CentralStockRow = { productId?: string; yarnType: string; stockKg: string };
/** Merkeze iplik girişinin tedarikçi bilgisi; çıkışlarda, sistem girişlerinde ve eski satırlarda yoktur. */
export type YarnReceiptSource = {
  supplierId?: string;
  supplierName: string;
  lotId?: string;
  lotNo?: string;
  waybillNo?: string;
};
export type CentralStockMovement = {
  id: string;
  productId?: string;
  yarnType: string;
  kind: CentralStockKind;
  netKg: string;
  note?: string;
  occurredAt: string;
  receipt?: YarnReceiptSource;
};
/** Bir tedarikçiden bir iplik cinsinden bugüne kadar gelen iplik. */
export type SupplierReceipt = {
  supplierId?: string;
  supplierName: string;
  productId?: string;
  yarnType: string;
  receivedKg: string;
};
export type YarnFlow = {
  /** Merkeze girenler ve kapıdan aktarılanlar. */
  receivedKg: string;
  /** Merkez depoya girmeden kapıdan fasoncuya aktarılan kısım. */
  directTransferKg: string;
  /** Fasoncuların elindeki iplik (bakiye toplamı). */
  atSubcontractorsKg: string;
  /** Merkez fabrikada kumaş üretimine verilen iplik. */
  usedInCentralProductionKg: string;
};
export type CentralStock = {
  rows: CentralStockRow[];
  totalKg: string;
  /** Merkez fabrikada bugüne kadar üretilen toplam kumaş. */
  producedFabricKg: string;
  /** Gelen ipliğin merkez / fasoncu / merkez üretimi dağılımı. */
  flow: YarnFlow;
  movements: CentralStockMovement[];
  /** Tedarikçi ve iplik cinsi başına gelen iplik; tedarikçi adına göre sıralı. */
  suppliers: SupplierReceipt[];
};

/**
 * `productId`: Tanımlar'daki aktif iplik, `supplierId`: aktif iplik tedarikçisi. `lotNo` yazılırsa Partiler'deki
 * partiye bağlanır; parti ilk kez yazılıyorsa `qualityGradeId` (kalite sınıfı) gerekir.
 */
export type YarnReceiptRequest = {
  productId: string;
  supplierId: string;
  netKg: string;
  lotNo?: string;
  qualityGradeId?: string;
  waybillNo?: string;
  note?: string;
};
export type CentralProductionRequest = {
  fabricType: string;
  /** Oran olarak, ör. "0.04". */
  fireRate: string;
  rollCount?: number;
  waybillNo?: string;
  yarns: { productId: string; netKg: string }[];
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
