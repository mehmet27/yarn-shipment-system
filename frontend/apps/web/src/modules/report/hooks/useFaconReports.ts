import type { FireReportParams, GetDashboardParams, OpeningImportRequest } from '@iplik/api-client';
import {
  getBalanceMatrix,
  getFireReport,
  getToInvoiceReport,
  getYarnVsFabricReport,
  importOpeningBalance,
  REPORTS_PATH,
} from '@iplik/api-client';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export function useBalanceMatrix(params: GetDashboardParams, isEnabled: boolean) {
  const query = { ...(params.siteId === undefined ? {} : { siteId: params.siteId }) };
  return useQuery({
    queryKey: [REPORTS_PATH, 'balance-matrix', query],
    queryFn: () => getBalanceMatrix(query),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useFireReport(params: FireReportParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'fire', params],
    queryFn: () => getFireReport(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useYarnVsFabricReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'yarn-vs-fabric', params],
    queryFn: () => getYarnVsFabricReport(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useToInvoiceReport(params: GetDashboardParams, isEnabled: boolean) {
  return useQuery({
    queryKey: [REPORTS_PATH, 'to-invoice', params],
    queryFn: () => getToInvoiceReport(params),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

export function useOpeningImport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: OpeningImportRequest) => importOpeningBalance(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [REPORTS_PATH] }),
  });
}
