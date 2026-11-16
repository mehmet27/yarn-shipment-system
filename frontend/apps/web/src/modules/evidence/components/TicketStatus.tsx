import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import type { FileProblem } from '../uploadTypes';

/** Seçilen dosyanın sorununu ve fotoğrafın henüz yüklenmediğini (onayda yüklenecek) gösterir. */
export function TicketStatus({
  problem,
  isSelected,
}: {
  problem: FileProblem | undefined;
  isSelected: boolean;
}) {
  const { t } = useTranslation();
  return (
    <>
      {problem !== undefined && <Alert color="red">{t(`ticket.fileErrors.${problem}`)}</Alert>}
      {isSelected && <Alert color="blue">{t('ticket.selected')}</Alert>}
    </>
  );
}
