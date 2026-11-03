import type { UnverifiedReason } from '@iplik/api-client';
import { declareTicketUnverified } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';
import type { TicketAttempt, TicketOutcome } from '../ticketTypes';
import { unreadableEvidenceId } from '../ticketTypes';

export type DeclareUnverifiedRequest = { evidenceId: string; reason: UnverifiedReason };

/** Üç denemede okunamayan fiş için gerekçeyi kaydeder; sunucu "doğrulanamadı" uyuşmazlığı açar (K0). */
export function useDeclareUnverified() {
  return useMutation({
    mutationFn: ({ evidenceId, reason }: DeclareUnverifiedRequest) =>
      declareTicketUnverified(evidenceId, { reason }),
  });
}

/**
 * Yedek yoldaki "gerekçeyle devam et": gerekçe son denemenin okunamayan fişine yazılır, kayıt
 * başarılıysa adım tamamlanır. Okunamayan fiş yoksa (ör. okuma servisi kapalı) yalnızca adım tamamlanır.
 */
export function useUnverifiedFallback(
  last: TicketAttempt | undefined,
  onOutcome: (outcome: TicketOutcome) => void,
) {
  const declare = useDeclareUnverified();
  const onReason = (reason: UnverifiedReason) => {
    const done = () => {
      onOutcome({ kind: 'UNVERIFIED', reason });
    };
    const evidenceId = unreadableEvidenceId(last);
    if (evidenceId === undefined) {
      done();
      return;
    }
    declare.mutate({ evidenceId, reason }, { onSuccess: done });
  };
  return { declare, onReason };
}
