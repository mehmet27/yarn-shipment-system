import { useTranslation } from '@iplik/i18n';
import { SectionCard, Stack } from '@iplik/ui';
import { PendingRow } from './PendingRow';
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
    <SectionCard
      title={t('shipment.pendingDecisions.title', { count: pending.length })}
      description={t('shipment.pendingDecisions.hint')}
      icon="discrepancies"
      testId="pending-weight-decisions"
    >
      <Stack gap={0}>
        {pending.map((item) => (
          <PendingRow
            key={item.shipmentId}
            to={`/panel/sevkiyat/${item.shipmentId}`}
            shipmentNo={item.shipmentNo}
            description={item.subcontractorTitle}
            color="red"
          />
        ))}
      </Stack>
    </SectionCard>
  );
}
