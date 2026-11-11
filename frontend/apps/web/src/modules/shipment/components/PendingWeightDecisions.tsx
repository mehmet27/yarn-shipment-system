import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, Group, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useShipments } from '../hooks/useShipments';

const LIST_SIZE = 20;

/**
 * Kantar uyuşmazlığında karar bekleyen sevkiyatlar. Müdürün panel ana sayfasındaki bildirimdir: yalnızca kararı verme
 * yetkisi olan kullanıcıya görünür, bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingWeightDecisions() {
  const { t } = useTranslation();
  const query = useShipments({ status: ['WEIGHT_MISMATCH'], size: LIST_SIZE });
  const pending = (query.data?.items ?? []).filter((item) =>
    item.allowedActions.includes('RESOLVE_WEIGHT_MISMATCH'),
  );
  if (pending.length === 0) return null;
  return (
    <Alert
      color="orange"
      title={t('shipment.pendingDecisions.title', { count: pending.length })}
      data-testid="pending-weight-decisions"
    >
      <Stack gap="xs">
        <Text size="sm">{t('shipment.pendingDecisions.hint')}</Text>
        {pending.map((item) => (
          <Group key={item.shipmentId} justify="space-between">
            <Anchor component={Link} to={`/panel/sevkiyat/${item.shipmentId}`}>
              {item.shipmentNo}
            </Anchor>
            <Text size="sm" c="dimmed">
              {item.subcontractorTitle}
            </Text>
          </Group>
        ))}
      </Stack>
    </Alert>
  );
}
