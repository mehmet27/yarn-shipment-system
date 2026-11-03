import type { RecordCorrectionRequest } from '@iplik/api-client';
import { listCorrections, recordCorrection, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export function useCorrections(shipmentId: string) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, shipmentId, 'corrections'],
    queryFn: () => listCorrections(shipmentId),
  });
}

/** Düzeltmeden sonra sevkiyat (tartı karşılaştırması) ve düzeltme listesi yeniden okunur. */
export function useRecordCorrection(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: RecordCorrectionRequest) => recordCorrection(shipmentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH, shipmentId] }),
  });
}
