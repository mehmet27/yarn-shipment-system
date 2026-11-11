import { customInstance } from './customInstance';
import type { GetDashboardParams } from './generated/reportingService';

const BASE = '/api/v1/reports';

export type BalanceMatrixCell = {
  yarnType: string;
  subcontractorId: string;
  subcontractorName: string;
  balanceKg: string;
  inTransitKg: string;
};

export type FireStatus = 'AWAITING_FABRIC' | 'TO_INVOICE' | 'CLOSED';

export type FireRowView = {
  shipmentId: string;
  shipmentNo: string;
  subcontractorId: string;
  subcontractorName: string;
  sentKg: string;
  returnedYarnKg: string;
  fabricKg: string;
  fireRate: string;
  expectedFabricKg: string;
  actualFireRate: string;
  shortageKg: string;
  status: FireStatus;
  invoiceNo?: string;
  invoicedAt?: string;
};

export type YarnVsFabricRowView = {
  subcontractorId: string;
  subcontractorName: string;
  sentKg: string;
  returnedYarnKg: string;
  fabricKg: string;
  actualFireRate: string;
  shortageKg: string;
};

export type FireReportParams = GetDashboardParams & { subcontractorId?: string };

export type OpeningImportRow = {
  subcontractorId: string;
  subcontractorName: string;
  yarnType: string;
  netKg: string;
};
export type OpeningImportRequest = { asOf: string; rows: OpeningImportRow[] };
export type OpeningImportResponse = { imported: number; skipped: number };

export const getBalanceMatrix = (params?: { siteId?: string }) =>
  customInstance<BalanceMatrixCell[]>({
    url: `${BASE}/balance-matrix`,
    method: 'GET',
    params,
  });

export const getFireReport = (params?: FireReportParams) =>
  customInstance<FireRowView[]>({ url: `${BASE}/fire`, method: 'GET', params });

export const getYarnVsFabricReport = (params?: GetDashboardParams) =>
  customInstance<YarnVsFabricRowView[]>({
    url: `${BASE}/yarn-vs-fabric`,
    method: 'GET',
    params,
  });

export const getToInvoiceReport = (params?: GetDashboardParams) =>
  customInstance<FireRowView[]>({ url: `${BASE}/to-invoice`, method: 'GET', params });

export const importOpeningBalance = (request: OpeningImportRequest) =>
  customInstance<OpeningImportResponse>({
    url: `${BASE}/balance/opening-import`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: request,
  });
