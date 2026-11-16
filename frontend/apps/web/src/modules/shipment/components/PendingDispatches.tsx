import { useTranslation } from '@iplik/i18n';
import { Anchor, Group, Paper, Stack, Text, Title } from '@iplik/ui';
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
    <Paper withBorder p="md" data-testid="pending-dispatches">
      <Stack gap="xs">
        <Title order={2} size="h4">
          {t('shipment.pendingDispatches.title', { count: pending.length })}
        </Title>
        <Text size="sm" c="dimmed">
          {t('shipment.pendingDispatches.hint')}
        </Text>
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
    </Paper>
  );
}
