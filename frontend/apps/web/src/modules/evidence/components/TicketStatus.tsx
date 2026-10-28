import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import type { TicketAttempt, TicketOutcome } from '../ticketTypes';
import { TicketReadingResult } from './TicketReadingResult';
import { TicketUnreadable } from './TicketUnreadable';

const NOTES = {
  SKIPPED: { key: 'ticket.skipped', color: 'blue' },
  DISPLAY_PHOTO: { key: 'ticket.displayDone', color: 'blue' },
  UNVERIFIED: { key: 'ticket.unverified', color: 'orange' },
} as const;

/** Son okuma sonucu ile adımın sonucunu (atlandı, ekran fotoğrafı, gerekçe) gösterir. */
export function TicketStatus({
  last,
  outcome,
}: {
  last: TicketAttempt | undefined;
  outcome: TicketOutcome | undefined;
}) {
  const { t } = useTranslation();
  const note = outcome === undefined || outcome.kind === 'READ' ? undefined : NOTES[outcome.kind];
  return (
    <>
      {last?.kind === 'READ' && <TicketReadingResult reading={last.reading} />}
      {last?.kind === 'UNREADABLE' && <TicketUnreadable reading={last.reading} />}
      {note !== undefined && <Alert color={note.color}>{t(note.key)}</Alert>}
    </>
  );
}
