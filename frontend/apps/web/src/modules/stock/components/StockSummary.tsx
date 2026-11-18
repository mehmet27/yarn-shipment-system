import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Paper } from '@iplik/ui';
import { StatCell, StatGrid } from '@/modules/report';
import { formatKgText, formatSignedKg } from '@/modules/shipment';

/** Üst özet: toplam iplik stoku, üretilen kumaş ve eksiye düşen iplik sayısı; çizgiyle ayrılmış hücreler. */
export function StockSummary({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const negative = stock.rows.filter((row) => row.stockKg.startsWith('-')).length;
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <StatGrid>
        <StatCell label={t('stock.summary.total')} value={formatSignedKg(stock.totalKg)} />
        <StatCell
          label={t('stock.summary.produced')}
          value={formatKgText(stock.producedFabricKg)}
        />
        <StatCell
          label={t('stock.summary.negative')}
          value={negative}
          color={negative > 0 ? 'red' : undefined}
          hint={negative > 0 ? t('stock.summary.negativeHint') : undefined}
        />
      </StatGrid>
    </Paper>
  );
}
