import type { ResolveDiscrepancyRequest } from '@iplik/api-client';
import {
  assignDiscrepancy,
  DISCREPANCIES_PATH,
  resolveDiscrepancy,
  SHIPMENTS_PATH,
} from '@iplik/api-client';
import type { InvoiceRequest } from '@iplik/api-client/settlements';
import {
  BALANCES_PATH,
  FIRE_SETTLEMENTS_PATH,
  getFireSettlement,
  markInvoiced,
} from '@iplik/api-client/settlements';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

/** Karar sonrası detay, geçmiş ve liste yeniden okunur (hepsi aynı anahtar önekinde). */
export function useRefreshDiscrepancies() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: [DISCREPANCIES_PATH] });
}

export function useAssignDiscrepancy(id: string) {
  const refresh = useRefreshDiscrepancies();
  return useMutation({
    mutationFn: (assigneeId: string) => assignDiscrepancy(id, { assigneeId }),
    onSuccess: refresh,
  });
}

export type ResolveInput = { request: ResolveDiscrepancyRequest; invoice?: InvoiceRequest };
export type ResolveResult = { isInvoiceFailed: boolean };

/** Fire dosyası fatura bekliyorsa "Haklı, faturalandı" seçeneği sunulur. */
export function useIsInvoiceable(shipmentId: string, isEnabled: boolean) {
  const query = useQuery({
    queryKey: [SHIPMENTS_PATH, shipmentId, 'fire-settlement'],
    queryFn: () => getFireSettlement(shipmentId),
    enabled: isEnabled,
  });
  return query.data?.status === 'TO_INVOICE';
}

/**
 * Karar kaydedilir; fatura da girildiyse hemen ardından işlenir. Fatura kaydı başarısız olursa karar
 * yine de geçerlidir: sonuç bunu bildirir, uyuşmazlık ekranı kullanıcı "Tamam" diyene kadar yenilenmez.
 */
export function useResolveDiscrepancy(id: string, shipmentId: string) {
  const queryClient = useQueryClient();
  const refreshDiscrepancies = useRefreshDiscrepancies();
  return useMutation({
    mutationFn: async ({ request, invoice }: ResolveInput): Promise<ResolveResult> => {
      await resolveDiscrepancy(id, request);
      if (invoice === undefined) return { isInvoiceFailed: false };
      try {
        await markInvoiced(shipmentId, invoice);
        return { isInvoiceFailed: false };
      } catch {
        return { isInvoiceFailed: true };
      }
    },
    onSuccess: async (result) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH, shipmentId] }),
        queryClient.invalidateQueries({ queryKey: [BALANCES_PATH] }),
        queryClient.invalidateQueries({ queryKey: [FIRE_SETTLEMENTS_PATH] }),
      ]);
      if (!result.isInvoiceFailed) await refreshDiscrepancies();
    },
  });
}
