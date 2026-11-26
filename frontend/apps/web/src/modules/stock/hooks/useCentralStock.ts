import type { CentralProductionRequest, YarnReceiptRequest } from '@iplik/api-client/central-stock';
import {
  CENTRAL_STOCK_PATH,
  getCentralStock,
  recordCentralProduction,
  recordYarnReceipt,
} from '@iplik/api-client/central-stock';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MASTER_DATA_PATH } from '@/modules/shipment';

export function useCentralStock() {
  return useQuery({ queryKey: [CENTRAL_STOCK_PATH], queryFn: getCentralStock });
}

function useRefreshStock() {
  const client = useQueryClient();
  return () => client.invalidateQueries({ queryKey: [CENTRAL_STOCK_PATH] });
}

export function useRecordYarnReceipt() {
  const refresh = useRefreshStock();
  const client = useQueryClient();
  return useMutation({
    mutationFn: (request: YarnReceiptRequest) => recordYarnReceipt(request),
    // Giriş yeni bir parti açmış olabilir; parti listeleri de yenilenir.
    onSuccess: () =>
      Promise.all([refresh(), client.invalidateQueries({ queryKey: [MASTER_DATA_PATH, 'lot'] })]),
  });
}

export function useRecordCentralProduction() {
  const refresh = useRefreshStock();
  return useMutation({
    mutationFn: (request: CentralProductionRequest) => recordCentralProduction(request),
    onSuccess: refresh,
  });
}
