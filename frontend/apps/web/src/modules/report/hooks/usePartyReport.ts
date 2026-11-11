import type { GetDashboardParams } from '@iplik/api-client';
import {
  getShipmentReport,
  getSubcontractorBalanceReport,
  getSubcontractorReport,
  REPORTS_PATH,
} from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export const REPORT_KINDS = [
  'summary',
  'subcontractors',
  'shipments',
  'balance',
  'balanceMatrix',
  'fire',
  'yarnVsFabric',
  'toInvoice',
] as const;
export type ReportKind = (typeof REPORT_KINDS)[number];

const FETCHERS = {
  subcontractors: getSubcontractorReport,
  shipments: getShipmentReport,
  balance: getSubcontractorBalanceReport,
} as const;

/** Tür başına tek sorgu; yalnızca açık sekmenin raporu çekilir. */
export function useSubcontractorReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'subcontractors', params],
    queryFn: () => FETCHERS.subcontractors(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useShipmentReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'shipments', params],
    queryFn: () => FETCHERS.shipments(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useBalanceReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'balance', params],
    queryFn: () => FETCHERS.balance(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}
