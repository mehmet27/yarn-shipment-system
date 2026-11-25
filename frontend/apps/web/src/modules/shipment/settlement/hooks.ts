import type { FabricPreviewRequest, FabricReceiptRequest } from '@iplik/api-client/fabric';
import {
  FABRIC_RECIPES_PATH,
  listFabricDraws,
  listFabricRecipes,
  listFabricReceipts,
  listFabricYarnOptions,
  previewFabricReceipt,
  recordFabricReceipt,
} from '@iplik/api-client/fabric';
import type {
  FireRateRequest,
  InvoiceRequest,
  YarnReturnRequest,
} from '@iplik/api-client/settlements';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import {
  BALANCES_PATH,
  closeFireSettlement,
  FIRE_INVOICES_PATH,
  FIRE_SETTLEMENTS_PATH,
  getFireSettlement,
  listYarnReturns,
  markInvoiced,
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

/** Fasoncunun elindeki iplikler (partisi ve tedarikçisiyle); kumaş oranları bu satırlara yazılır. */
export const useFabricYarnOptions = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'fabric-yarn-options'),
    queryFn: () => listFabricYarnOptions(shipmentId),
  });

/** Bu sevkiyatın ipliğinden üretilen kumaşlar. */
export const useFabricDraws = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'fabric-draws'),
    queryFn: () => listFabricDraws(shipmentId),
  });

/** Kumaş cinsi başına tanımlı karışımlar (yalnız aktifler). */
export const useActiveFabricRecipes = () =>
  useQuery({
    queryKey: [FABRIC_RECIPES_PATH, 'active'],
    queryFn: () => listFabricRecipes(false),
  });

export function usePreviewFabricReceipt(shipmentId: string) {
  return useMutation({
    mutationFn: (request: FabricPreviewRequest) => previewFabricReceipt(shipmentId, request),
  });
}

export const useYarnReturns = (shipmentId: string) =>
  useQuery({
    queryKey: key(shipmentId, 'yarn-returns'),
    queryFn: () => listYarnReturns(shipmentId),
  });

export const useFireSettlement = (shipmentId: string, isEnabled = true) =>
  useQuery({
    queryKey: key(shipmentId, 'fire-settlement'),
    queryFn: () => getFireSettlement(shipmentId),
    enabled: isEnabled,
  });

/** Kumaş, iplik dönüşü ve fire değişince fire dosyası, bakiyeler ve faturalanacaklar yeniden okunur. */
function useRefresh(shipmentId: string) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH, shipmentId] }),
      queryClient.invalidateQueries({ queryKey: [BALANCES_PATH] }),
      queryClient.invalidateQueries({ queryKey: [FIRE_SETTLEMENTS_PATH] }),
      queryClient.invalidateQueries({ queryKey: [FIRE_INVOICES_PATH] }),
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
