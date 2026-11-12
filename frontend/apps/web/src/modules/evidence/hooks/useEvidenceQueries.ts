import { getEvidence, getEvidenceDownloadUrl, listEvidence } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export const EVIDENCE_QUERY_KEY = 'evidence';

/** İndirme adresi yaklaşık 5 dk geçerli; 4 dk sonra yenilenir. */
const URL_STALE_MS = 240_000;

/** Sevkiyatın görmeye izinli olunan bütün kanıtları. */
export function useShipmentEvidence(shipmentId: string) {
  return useQuery({
    queryKey: [EVIDENCE_QUERY_KEY, 'shipment', shipmentId],
    queryFn: () => listEvidence({ shipmentId }),
    select: (view) => view.items,
  });
}

/** Tek kanıtın üst verisi (itiraz, lab sonucu, uyuşmazlık etkinliği gibi yalnız kimliğin bilindiği yerler). */
export function useEvidence(evidenceId: string) {
  return useQuery({
    queryKey: [EVIDENCE_QUERY_KEY, 'one', evidenceId],
    queryFn: () => getEvidence(evidenceId),
  });
}

/** Kısa ömürlü dosya adresi; saklanmaz, süresi dolmadan yenilenir. */
export function useEvidenceUrl(evidenceId: string, enabled = true) {
  return useQuery({
    queryKey: [EVIDENCE_QUERY_KEY, 'url', evidenceId],
    queryFn: () => getEvidenceDownloadUrl(evidenceId),
    enabled,
    staleTime: URL_STALE_MS,
    gcTime: URL_STALE_MS,
  });
}
