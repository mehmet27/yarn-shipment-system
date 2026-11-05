import { useTranslation } from '@iplik/i18n';
import { ActivityFeed, Text } from '@iplik/ui';
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
          <ActivityFeed
            label={t('shipment.history.label')}
            items={items.map((change) => ({
              id: `${change.changedAt}-${change.toStatus}`,
              title:
                change.fromStatus === undefined
                  ? t(`shipment.status.${change.toStatus}`)
                  : `${t(`shipment.status.${change.fromStatus}`)} → ${t(`shipment.status.${change.toStatus}`)}`,
              meta: [
                `${formatDateTime(change.changedAt)} · ${actorLabel(change.changedBy, change.changedByName)}`,
                change.reason,
              ]
                .filter((part) => part !== undefined)
                .join(' · '),
            }))}
          />
        )
      }
    </QueryState>
  );
}
