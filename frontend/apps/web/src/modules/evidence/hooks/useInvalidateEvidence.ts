import type { EvidenceView } from '@iplik/api-client';
import { invalidateEvidence } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { EVIDENCE_QUERY_KEY } from './useEvidenceQueries';

/** Admin: kanıtı gerekçeyle geçersiz kılar; kanıt listeleri ve tek kanıt yenilenir. */
export function useInvalidateEvidence(evidenceId: string) {
  const queryClient = useQueryClient();
  return useMutation<EvidenceView, Error, string>({
    mutationFn: (reason) => invalidateEvidence(evidenceId, { reason }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [EVIDENCE_QUERY_KEY] }),
  });
}
