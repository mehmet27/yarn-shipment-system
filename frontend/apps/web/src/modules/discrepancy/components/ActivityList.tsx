import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text, Title } from '@iplik/ui';
import { QueryState, useActorLabel } from '@/modules/shipment';
import { useDiscrepancyActivity } from '../hooks/useDiscrepancy';
import { ruleLabel } from './discrepancyText';

const KNOWN_ACTIONS = ['OPENED', 'ASSIGNED', 'RESOLVED'];

/** Etkinlik geçmişi en eskiden yeniye; bilinmeyen eylem türü ham değerle gösterilir. */
export function ActivityList({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const query = useDiscrepancyActivity(discrepancyId);
  const actorLabel = useActorLabel();
  return (
    <Stack gap="xs">
      <Title order={4}>{t('discrepancy.activity.title')}</Title>
      <QueryState query={query}>
        {(items) =>
          items.map((item) => (
            <Paper key={item.id} withBorder p="sm">
              <Text fw={600}>
                {KNOWN_ACTIONS.includes(item.actionType)
                  ? t(`discrepancy.activity.${item.actionType}`)
                  : item.actionType}
              </Text>
              {item.note !== undefined && (
                <Text size="sm">
                  {item.actionType === 'OPENED' ? ruleLabel(t, item.note) : item.note}
                </Text>
              )}
              <Text size="sm" c="dimmed">
                {formatDateTime(item.occurredAt)} · {actorLabel(item.actorId)}
              </Text>
            </Paper>
          ))
        }
      </QueryState>
    </Stack>
  );
}
