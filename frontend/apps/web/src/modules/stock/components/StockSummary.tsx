import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { SimpleGrid, StatCard } from '@iplik/ui';
import { formatKgText, formatSignedKg } from '@/modules/shipment';

/** Üst özet: toplam iplik stoku, üretilen kumaş ve eksiye düşen iplik sayısı. */
export function StockSummary({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const negative = stock.rows.filter((row) => row.stockKg.startsWith('-')).length;
  return (
    <SimpleGrid cols={{ base: 1, sm: 3 }}>
      <StatCard
        label={t('stock.summary.total')}
        value={formatSignedKg(stock.totalKg)}
        tone="info"
      />
      <StatCard label={t('stock.summary.produced')} value={formatKgText(stock.producedFabricKg)} />
      <StatCard
        label={t('stock.summary.negative')}
        value={negative}
        tone={negative > 0 ? 'danger' : 'success'}
        {...(negative > 0 ? { hint: t('stock.summary.negativeHint') } : {})}
      />
    </SimpleGrid>
  );
}
