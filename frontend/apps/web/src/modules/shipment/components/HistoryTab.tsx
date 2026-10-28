import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { useShipmentHistory } from '../hooks/useShipmentHistory';
import { formatDateTime } from './formatDateTime';
import { QueryState } from './QueryState';
import { useActorLabel } from './useActorLabel';

/** Durum geçmişi eskiden yeniye; mobilde her geçiş bir kart olarak alt alta dizilir. */
export function HistoryTab({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const query = useShipmentHistory(shipmentId);
  const actorLabel = useActorLabel();
  return (
    <QueryState query={query}>
      {({ items }) =>
        items.length === 0 ? (
          <Text c="dimmed">{t('shipment.history.empty')}</Text>
        ) : (
          <Stack gap="xs">
            {items.map((change) => (
              <Paper key={`${change.changedAt}-${change.toStatus}`} withBorder p="sm">
                <Text fw={600}>
                  {change.fromStatus === undefined
                    ? t(`shipment.status.${change.toStatus}`)
                    : `${t(`shipment.status.${change.fromStatus}`)} → ${t(`shipment.status.${change.toStatus}`)}`}
                </Text>
                <Text size="sm" c="dimmed">
                  {formatDateTime(change.changedAt)} · {actorLabel(change.changedBy)}
                </Text>
                {change.reason !== undefined && <Text size="sm">{change.reason}</Text>}
              </Paper>
            ))}
          </Stack>
        )
      }
    </QueryState>
  );
}
