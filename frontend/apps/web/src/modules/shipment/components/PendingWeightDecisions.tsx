import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, Group, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { usePendingWeightDecisions } from '../hooks/usePendingWeightDecisions';

/**
 * Kantar uyuşmazlığında karar bekleyen sevkiyatlar. Müdürün panel ana sayfasındaki bildirimdir: yalnızca kararı verme
 * yetkisi olan kullanıcıya görünür, bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingWeightDecisions() {
  const { t } = useTranslation();
  const pending = usePendingWeightDecisions();
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
