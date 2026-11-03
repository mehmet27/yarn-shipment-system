import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Title } from '@iplik/ui';
import { useDiscrepancyTickets, useMarkTicketMisread } from '../hooks/useDiscrepancyTickets';
import { TicketCard } from './TicketCard';

/**
 * Sevkiyatın kantar fişleri: fotoğraf, AI'ın okuduğu ve elle girilen net yan yana.
 * Liste boşsa (fiş yok ya da kör giriş gereği gizli) panel hiç görünmez.
 */
export function TicketPanel({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const query = useDiscrepancyTickets(discrepancyId);
  const mark = useMarkTicketMisread(discrepancyId);
  if (query.data === undefined || query.data.length === 0) return null;
  return (
    <Paper withBorder p="md">
      <Stack gap="md">
        <Title order={2} size="h4">
          {t('discrepancy.tickets.title')}
        </Title>
        {mark.isError && <Alert color="red">{t('discrepancy.tickets.markFailed')}</Alert>}
        {query.data.map((ticket) => (
          <TicketCard
            key={ticket.evidenceId}
            ticket={ticket}
            marking={mark.isPending && mark.variables === ticket.evidenceId}
            onMark={() => {
              mark.mutate(ticket.evidenceId);
            }}
          />
        ))}
      </Stack>
    </Paper>
  );
}
