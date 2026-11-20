import { useTranslation } from '@iplik/i18n';
import { SectionCard, Stack, Text } from '@iplik/ui';
import { PendingRow } from '@/modules/shipment';
import { usePendingFabric } from '../hooks/usePendingSettlements';

/**
 * Kalitenin ana sayfadaki iş listesi: fasoncuya teslim edilmiş, kumaşı henüz girilmemiş sevkiyatlar. Satır, dosyayı
 * "Kumaş girişi" sekmesinde açar. Bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingFabric() {
  const { t } = useTranslation();
  const pending = usePendingFabric();
  if (pending.items.length === 0) return null;
  return (
    <SectionCard
      title={t('shipment.pendingFabric.title', { count: pending.total })}
      description={t('shipment.pendingFabric.hint')}
      icon="lots"
      testId="pending-fabric"
    >
      <Stack gap={0}>
        {pending.items.map((item) => (
          <PendingRow
            key={item.shipmentId}
            to={`/panel/sevkiyat/${item.shipmentId}?sekme=kumas`}
            shipmentNo={item.shipmentNo}
            description={item.subcontractorTitle}
            color="yellow"
          />
        ))}
        {pending.total > pending.items.length && (
          <Text size="sm" c="dimmed" pt="xs">
            {t('shipment.pendingFabric.more', { shown: pending.items.length })}
          </Text>
        )}
      </Stack>
    </SectionCard>
  );
}
