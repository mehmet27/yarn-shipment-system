import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text, Title } from '@iplik/ui';
import { EvidenceById } from '@/modules/evidence';
import { QueryState, useActorLabel } from '@/modules/shipment';
import type { DiscrepancyActivityView } from '@iplik/api-client';
import { useDiscrepancyActivity } from '../hooks/useDiscrepancy';
import { ruleLabel } from './discrepancyText';

const KNOWN_ACTIONS = [
  'OPENED',
  'ASSIGNED',
  'RESOLVED',
  'CLAIM_EVIDENCE',
  'INSPECTION_REQUESTED',
  'INSPECTION_RESULT',
];

/** Etkinlik geçmişi en eskiden yeniye; bilinmeyen eylem türü ham değerle gösterilir. */
export function ActivityList({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const query = useDiscrepancyActivity(discrepancyId);
  const actorLabel = useActorLabel();
  /** Açılışta kural koduna, atamada atanan kişinin adına çevrilir; diğer notlar olduğu gibi gösterilir. */
  const noteText = (item: DiscrepancyActivityView, note: string): string => {
    if (item.actionType === 'OPENED') return ruleLabel(t, note);
    return item.actionType === 'ASSIGNED' ? actorLabel(note, item.assigneeName) : note;
  };
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
              {item.note !== undefined && <Text size="sm">{noteText(item, item.note)}</Text>}
              {item.evidenceId !== undefined && <EvidenceById evidenceId={item.evidenceId} />}
              <Text size="sm" c="dimmed">
                {formatDateTime(item.occurredAt)} · {actorLabel(item.actorId, item.actorName)}
              </Text>
            </Paper>
          ))
        }
      </QueryState>
    </Stack>
  );
}
