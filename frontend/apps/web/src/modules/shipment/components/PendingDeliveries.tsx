import { useTranslation } from '@iplik/i18n';
import { SectionCard, Stack } from '@iplik/ui';
import { PendingRow } from './PendingRow';
import { usePendingDeliveries } from '../hooks/usePendingShipments';

/**
 * Müdürün ana sayfadaki iş listesi: yolda olan ve irsaliyeyle tamamlanmayı bekleyen sevkiyatlar. Yalnızca tamamlama
 * yetkisi olan kullanıcıya görünür; bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingDeliveries() {
  const { t } = useTranslation();
  const pending = usePendingDeliveries().items;
  if (pending.length === 0) return null;
  return (
    <SectionCard
      title={t('shipment.pendingDeliveries.title', { count: pending.length })}
      description={t('shipment.pendingDeliveries.hint')}
      icon="truck"
      testId="pending-deliveries"
    >
      <Stack gap={0}>
        {pending.map((item) => (
          <PendingRow
            key={item.shipmentId}
            to={`/panel/sevkiyat/${item.shipmentId}`}
            shipmentNo={item.shipmentNo}
            description={item.subcontractorTitle}
            color="yellow"
          />
        ))}
      </Stack>
    </SectionCard>
  );
}
