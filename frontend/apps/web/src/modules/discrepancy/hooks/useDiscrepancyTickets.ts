import {
  DISCREPANCIES_PATH,
  getEvidenceDownloadUrl,
  listDiscrepancyTickets,
  markTicketMisread,
} from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

/** İmzalı indirme adresi ~5 dk geçerli; daha erken yenilenir. */
const PHOTO_URL_STALE_MS = 240_000;

export function useDiscrepancyTickets(id: string) {
  return useQuery({
    queryKey: [DISCREPANCIES_PATH, 'tickets', id],
    queryFn: () => listDiscrepancyTickets(id),
  });
}

/** "AI yanıldı": işaret sonrası fiş paneli ve geçmiş yeniden okunur. */
export function useMarkTicketMisread(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (evidenceId: string) => markTicketMisread(id, evidenceId, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [DISCREPANCIES_PATH] }),
  });
}

/** Fiş fotoğrafının kısa ömürlü adresi; görme kuralları evidence-service'tedir. */
export function useTicketPhotoUrl(evidenceId: string) {
  return useQuery({
    queryKey: ['evidence', 'download-url', evidenceId],
    queryFn: () => getEvidenceDownloadUrl(evidenceId),
    staleTime: PHOTO_URL_STALE_MS,
  });
}
