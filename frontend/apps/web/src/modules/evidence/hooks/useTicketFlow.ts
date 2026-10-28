import { useState } from 'react';
import { checkImageQuality } from '../imageQuality';
import type { QualityIssue } from '../imageQuality';
import type { TicketAttempt, TicketOutcome } from '../ticketTypes';
import type { ReadTicketRequest } from './useReadTicket';
import { useReadTicket } from './useReadTicket';

function outcomeOf(attempt: TicketAttempt, read: boolean): TicketOutcome | undefined {
  if (!read) return { kind: 'DISPLAY_PHOTO' };
  if (attempt.kind === 'READ') return { kind: 'READ' };
  return attempt.kind === 'SKIPPED' ? { kind: 'SKIPPED' } : undefined;
}

/** Çekim durumu: seçilen dosya, kalite uyarıları, deneme sayısı ve son okuma. */
export function useTicketFlow(
  shipmentId: string,
  evidenceType: ReadTicketRequest['evidenceType'],
  onOutcome: (outcome: TicketOutcome | undefined) => void,
) {
  const mutation = useReadTicket(shipmentId);
  const [file, setFile] = useState<File>();
  const [issues, setIssues] = useState<readonly QualityIssue[]>([]);
  const [attempts, setAttempts] = useState(0);
  const [last, setLast] = useState<TicketAttempt>();
  const pick = (picked: File) => {
    setFile(picked);
    setLast(undefined);
    onOutcome(undefined);
    void checkImageQuality(picked).then(setIssues);
  };
  const send = (target: File, read: boolean) => {
    mutation.mutate(
      { file: target, evidenceType, read },
      {
        onSuccess: (attempt) => {
          setAttempts(attempts + 1);
          setLast(attempt);
          setFile(undefined);
          onOutcome(outcomeOf(attempt, read));
        },
      },
    );
  };
  return { mutation, file, issues, attempts, last, pick, send };
}
