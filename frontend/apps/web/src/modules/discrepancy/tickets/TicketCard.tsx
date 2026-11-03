import type { DiscrepancyTicketView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Paper, SimpleGrid, Stack, Text } from '@iplik/ui';
import { Fact } from '../components/Fact';
import { TicketPhoto } from './TicketPhoto';
import { enteredNet, netDifference, readValues } from './ticketText';

/** Tek fiş: fotoğraf solda, okunan/girilen değerler sağda; işaret yetkisi `allowedActions`'tan. */
export function TicketCard({
  ticket,
  marking,
  onMark,
}: {
  ticket: DiscrepancyTicketView;
  marking: boolean;
  onMark: () => void;
}) {
  const { t } = useTranslation();
  const isRead = ticket.status === 'READ';
  return (
    <Paper withBorder p="sm" data-testid={`ticket-${ticket.evidenceId}`}>
      <Stack gap="sm">
        <Group justify="space-between">
          <Text fw={600}>{t(`discrepancy.tickets.side.${ticket.side}`)}</Text>
          <Group gap="xs">
            <Badge color={isRead ? 'blue' : 'gray'}>
              {t(isRead ? 'discrepancy.tickets.read' : 'discrepancy.tickets.unreadable')}
            </Badge>
            {ticket.misread !== undefined && (
              <Badge color="orange">{t('discrepancy.tickets.misread')}</Badge>
            )}
          </Group>
        </Group>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TicketPhoto evidenceId={ticket.evidenceId} />
          <Stack gap="xs">
            <Fact label={t('discrepancy.tickets.aiRead')} value={readValues(t, ticket)} />
            <Fact label={t('discrepancy.tickets.entered')} value={enteredNet(ticket)} />
            <Fact label={t('discrepancy.tickets.difference')} value={netDifference(ticket)} />
            <Fact label={t('discrepancy.tickets.plate')} value={ticket.plate} />
            <Fact label={t('discrepancy.tickets.ticketNo')} value={ticket.ticketNo} />
            {ticket.problems.map((problem) => (
              <Text key={problem} size="sm" c="orange">
                {t(`ticket.problems.${problem}`, { defaultValue: problem })}
              </Text>
            ))}
            {ticket.misread !== undefined && (
              <Text size="sm" c="dimmed">
                {t('discrepancy.tickets.misreadAt', {
                  at: formatDateTime(ticket.misread.markedAt),
                })}
              </Text>
            )}
          </Stack>
        </SimpleGrid>
        {ticket.allowedActions.includes('MARK_MISREAD') && (
          <Group>
            <Button variant="light" color="orange" loading={marking} onClick={onMark}>
              {t('discrepancy.tickets.markMisread')}
            </Button>
          </Group>
        )}
      </Stack>
    </Paper>
  );
}
