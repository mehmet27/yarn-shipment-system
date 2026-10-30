import type { RecordInspectionResultRequest, RequestInspectionRequest } from '@iplik/api-client';
import {
  DISCREPANCIES_PATH,
  getShipmentClaims,
  openQualityClaim,
  QUALITY_CLAIMS_PATH,
  recordInspectionResult,
  requestInspection,
} from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export type OpenClaimInput = { description: string; bagKeys: string[]; evidenceIds: string[] };

export function useShipmentClaims(shipmentId: string) {
  return useQuery({
    queryKey: [QUALITY_CLAIMS_PATH, 'shipment', shipmentId],
    queryFn: () => getShipmentClaims(shipmentId),
  });
}

/** İtiraz bir uyuşmazlık açar; itiraz dosyası ve uyuşmazlık ekranları yeniden okunur. */
function useRefresh() {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: [QUALITY_CLAIMS_PATH] }),
      queryClient.invalidateQueries({ queryKey: [DISCREPANCIES_PATH] }),
    ]);
}

export function useOpenClaim(shipmentId: string) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (input: OpenClaimInput) => openQualityClaim({ shipmentId, ...input }),
    onSuccess: refresh,
  });
}

export function useRequestInspection(claimId: string) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (request: RequestInspectionRequest) => requestInspection(claimId, request),
    onSuccess: refresh,
  });
}

export function useRecordInspectionResult(claimId: string, inspectionId: string) {
  const refresh = useRefresh();
  return useMutation({
    mutationFn: (request: RecordInspectionResultRequest) =>
      recordInspectionResult(claimId, inspectionId, request),
    onSuccess: refresh,
  });
}
