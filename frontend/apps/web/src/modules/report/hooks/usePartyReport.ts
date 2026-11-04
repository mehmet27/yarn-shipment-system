import type { GetDashboardParams } from '@iplik/api-client';
import {
  getDriverReport,
  getPairReport,
  getShipmentReport,
  getSubcontractorBalanceReport,
  getSubcontractorReport,
  REPORTS_PATH,
} from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export const REPORT_KINDS = [
  'summary',
  'drivers',
  'subcontractors',
  'pairs',
  'shipments',
  'balance',
] as const;
export type ReportKind = (typeof REPORT_KINDS)[number];

const FETCHERS = {
  drivers: getDriverReport,
  subcontractors: getSubcontractorReport,
  pairs: getPairReport,
  shipments: getShipmentReport,
  balance: getSubcontractorBalanceReport,
} as const;

/** Tür başına tek sorgu; yalnızca açık sekmenin raporu çekilir. */
export function useDriverReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'drivers', params],
    queryFn: () => FETCHERS.drivers(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useSubcontractorReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'subcontractors', params],
    queryFn: () => FETCHERS.subcontractors(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function usePairReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'pairs', params],
    queryFn: () => FETCHERS.pairs(params),
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
