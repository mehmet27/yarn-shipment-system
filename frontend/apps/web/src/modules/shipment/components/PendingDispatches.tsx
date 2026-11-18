import { useTranslation } from '@iplik/i18n';
import { Anchor, Group, SectionCard, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useShipments } from '../hooks/useShipments';

const LIST_SIZE = 20;

/**
 * Depocunun ana sayfadaki iş listesi: çıkış tartısı bekleyen sevkiyatlar ve sırası (1 çuvalları ekle, 2 tartıyı gir).
 * Yalnızca tartı girme yetkisi olan kullanıcıya görünür; bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingDispatches() {
  const { t } = useTranslation();
  const query = useShipments({ status: ['DRAFT'], size: LIST_SIZE });
  const pending = (query.data?.items ?? []).filter((item) =>
    item.allowedActions.includes('RECORD_DISPATCH'),
  );
  if (pending.length === 0) return null;
  return (
    <SectionCard
      title={t('shipment.pendingDispatches.title', { count: pending.length })}
      description={t('shipment.pendingDispatches.hint')}
      icon="truck"
      testId="pending-dispatches"
    >
      <Stack gap="xs">
        {pending.map((item) => (
          <Group key={item.shipmentId} justify="space-between" gap="xs">
            <Anchor component={Link} to={`/panel/sevkiyat/${item.shipmentId}`}>
              {item.shipmentNo}
            </Anchor>
            <Text size="sm" c="dimmed">
              {item.subcontractorTitle}
            </Text>
          </Group>
        ))}
      </Stack>
    </SectionCard>
  );
}
