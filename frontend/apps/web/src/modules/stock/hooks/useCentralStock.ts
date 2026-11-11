import type { CentralProductionRequest, YarnReceiptRequest } from '@iplik/api-client/central-stock';
import {
  CENTRAL_STOCK_PATH,
  getCentralStock,
  recordCentralProduction,
  recordYarnReceipt,
} from '@iplik/api-client/central-stock';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export function useCentralStock() {
  return useQuery({ queryKey: [CENTRAL_STOCK_PATH], queryFn: getCentralStock });
}

function useRefreshStock() {
  const client = useQueryClient();
  return () => client.invalidateQueries({ queryKey: [CENTRAL_STOCK_PATH] });
}

export function useRecordYarnReceipt() {
  const refresh = useRefreshStock();
  return useMutation({
    mutationFn: (request: YarnReceiptRequest) => recordYarnReceipt(request),
    onSuccess: refresh,
  });
}

export function useRecordCentralProduction() {
  const refresh = useRefreshStock();
  return useMutation({
    mutationFn: (request: CentralProductionRequest) => recordCentralProduction(request),
    onSuccess: refresh,
  });
}
