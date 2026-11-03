import type { ReadingProblem, TicketReadingView, UnverifiedReason } from '@iplik/api-client';

/** Fiş adımının sonucu: okundu, okuma servisi kapalı olduğu için atlandı, fotoğraf yeterli sayıldı ya da gerekçeyle devam edildi. */
export type TicketOutcome =
  | { kind: 'READ' }
  | { kind: 'SKIPPED' }
  | { kind: 'DISPLAY_PHOTO' }
  | { kind: 'UNVERIFIED'; reason: UnverifiedReason };

/** Servisin okumasına göre aşama: okunamadı sonucu da bir sonuçtur, hata değildir. */
export type TicketAttempt =
  | { kind: 'READ'; reading: TicketReadingView }
  | { kind: 'UNREADABLE'; reading: TicketReadingView }
  | { kind: 'SKIPPED' };

/** En fazla bu kadar fiş denemesinden sonra yedek yola geçilir. */
export const MAX_ATTEMPTS = 3;

/** Bunlar varsa fiş yeniden çekilmeden devam edilemez; diğer sorunlar yalnızca bilgidir. */
export const RETAKE_PROBLEMS: readonly ReadingProblem[] = [
  'WEIGHT_NOT_READ',
  'WEIGHT_INCONSISTENT',
  'ARITHMETIC_MISMATCH',
];

export const UNVERIFIED_REASONS: readonly UnverifiedReason[] = [
  'PRINTER_BROKEN',
  'TICKET_DAMAGED',
  'NO_TICKET',
  'OTHER',
];

/** Seçim kutusundan gelen değeri gerekçeye çevirir; bilinmeyen değer yok sayılır. */
export function toUnverifiedReason(value: string | null): UnverifiedReason | undefined {
  return UNVERIFIED_REASONS.find((reason) => reason === value);
}

/** Son denemenin okunamayan fişi; gerekçe bu kanıta yazılır. */
export function unreadableEvidenceId(last: TicketAttempt | undefined): string | undefined {
  return last?.kind === 'UNREADABLE' ? last.reading.evidenceId : undefined;
}

export function needsRetake(reading: TicketReadingView): boolean {
  return (
    reading.status === 'UNREADABLE' ||
    reading.problems.some((problem) => RETAKE_PROBLEMS.includes(problem))
  );
}
