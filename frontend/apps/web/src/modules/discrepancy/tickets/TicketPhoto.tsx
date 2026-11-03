import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';
import { useTicketPhotoUrl } from '../hooks/useDiscrepancyTickets';

/** Fiş fotoğrafı; görülemiyorsa (ör. kör giriş gereği) yalnızca not gösterilir. */
export function TicketPhoto({ evidenceId }: { evidenceId: string }) {
  const { t } = useTranslation();
  const query = useTicketPhotoUrl(evidenceId);
  if (query.isPending) return null;
  if (query.isError) {
    return (
      <Text size="sm" c="dimmed">
        {t('discrepancy.tickets.photoUnavailable')}
      </Text>
    );
  }
  return (
    <a href={query.data.url} target="_blank" rel="noreferrer">
      <img
        src={query.data.url}
        alt={t('discrepancy.tickets.photoAlt')}
        style={{ maxWidth: '100%', maxHeight: 240, objectFit: 'contain' }}
      />
    </a>
  );
}
