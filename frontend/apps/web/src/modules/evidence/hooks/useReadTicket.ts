import type { EvidenceSource, EvidenceType } from '@iplik/api-client';
import { readEvidenceTicket, toProblem } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';
import { needsRetake } from '../ticketTypes';
import type { TicketAttempt } from '../ticketTypes';
import { useUploadEvidence } from './useUploadEvidence';

export type ReadTicketRequest = { file: File; evidenceType: EvidenceType; read: boolean };

const CAMERA: EvidenceSource = 'CAMERA';

/** Fişi kanıt olarak yükler, ardından okutur; her çekim yeni bir kanıt kaydıdır. Okuma servisi kapalıysa atlanır. */
export function useReadTicket(shipmentId: string) {
  const upload = useUploadEvidence(shipmentId);
  return useMutation<TicketAttempt, Error, ReadTicketRequest>({
    mutationFn: async ({ file, evidenceType, read }) => {
      const evidence = await upload.mutateAsync({ file, evidenceType, source: CAMERA });
      if (!read) return { kind: 'SKIPPED' };
      try {
        const reading = await readEvidenceTicket(evidence.id);
        return needsRetake(reading) ? { kind: 'UNREADABLE', reading } : { kind: 'READ', reading };
      } catch (error) {
        if (toProblem(error).code === 'TICKET_READING_UNAVAILABLE') return { kind: 'SKIPPED' };
        throw error;
      }
    },
  });
}
