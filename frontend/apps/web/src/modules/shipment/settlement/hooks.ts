import type {
  FabricReceiptRequest,
  FireRateRequest,
  InvoiceRequest,
  YarnReturnRequest,
} from '@iplik/api-client/settlements';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import {
  BALANCES_PATH,
  closeFireSettlement,
  FIRE_SETTLEMENTS_PATH,
  getFireSettlement,
  listFabricReceipts,
  listYarnReturns,
  markInvoiced,
  recordFabricReceipt,
  recordYarnReturn,
  updateFireRate,
} from '@iplik/api-client/settlements';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

const key = (shipmentId: string, part: string) => [SHIPMENTS_PATH, shipmentId, part] as const;

export const useFabricReceipts = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'fabric-receipts'),
    queryFn: () => listFabricReceipts(shipmentId),
  });

export const useYarnReturns = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'yarn-returns'),
    queryFn: () => listYarnReturns(shipmentId),
  });

export const useFireSettlement = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'fire-settlement'),
    queryFn: () => getFireSettlement(shipmentId),
  });

/** Kumaş, iplik dönüşü ve fire değişince fire dosyası, bakiyeler ve faturalanacaklar yeniden okunur. */
function useRefresh(shipmentId: string) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH, shipmentId] }),
      queryClient.invalidateQueries({ queryKey: [BALANCES_PATH] }),
      queryClient.invalidateQueries({ queryKey: [FIRE_SETTLEMENTS_PATH] }),
    ]);
}

export function useRecordFabricReceipt(shipmentId: string) {
  const refresh = useRefresh(shipmentId);
  return useMutation({
    mutationFn: (request: FabricReceiptRequest) => recordFabricReceipt(shipmentId, request),
    onSuccess: refresh,
  });
}

export function useRecordYarnReturn(shipmentId: string) {
  const refresh = useRefresh(shipmentId);
  return useMutation({
    mutationFn: (request: YarnReturnRequest) => recordYarnReturn(shipmentId, request),
    onSuccess: refresh,
  });
}

export function useUpdateFireRate(shipmentId: string) {
  const refresh = useRefresh(shipmentId);
  return useMutation({
    mutationFn: (request: FireRateRequest) => updateFireRate(shipmentId, request),
    onSuccess: refresh,
  });
}

export function useMarkInvoiced(shipmentId: string) {
  const refresh = useRefresh(shipmentId);
  return useMutation({
    mutationFn: (request: InvoiceRequest) => markInvoiced(shipmentId, request),
    onSuccess: refresh,
  });
}

export function useCloseFireSettlement(shipmentId: string) {
  const refresh = useRefresh(shipmentId);
  return useMutation({ mutationFn: () => closeFireSettlement(shipmentId), onSuccess: refresh });
}
