import {
  DISCREPANCIES_PATH,
  getShipmentClaims,
  openQualityClaim,
  QUALITY_CLAIMS_PATH,
} from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export type OpenClaimInput = { description: string; claimedKg: string; evidenceIds: string[] };

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
