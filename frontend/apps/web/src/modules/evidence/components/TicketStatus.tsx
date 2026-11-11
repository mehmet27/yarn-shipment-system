import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import type { TicketOutcome } from '../ticketTypes';
import type { FileProblem } from '../uploadTypes';

/** Seçilen dosyanın sorununu ve fotoğrafın kaydedildiğini gösterir. */
export function TicketStatus({
  problem,
  outcome,
}: {
  problem: FileProblem | undefined;
  outcome: TicketOutcome | undefined;
}) {
  const { t } = useTranslation();
  return (
    <>
      {problem !== undefined && <Alert color="red">{t(`ticket.fileErrors.${problem}`)}</Alert>}
      {outcome !== undefined && <Alert color="green">{t('ticket.photoDone')}</Alert>}
    </>
  );
}
