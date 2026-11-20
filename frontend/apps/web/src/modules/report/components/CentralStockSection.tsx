import { useTranslation } from '@iplik/i18n';
import { BarList, SectionCard, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { foldOthers, toKgBars } from '../charts/barItems';
import { formatKilograms } from '../format';
import { useCentralStockTotals } from '../hooks/useStockOverview';

/** "Merkez depoda hangi iplikten ne kadar var?": Merkez iplik stoku ekranındaki satırlar, en çok olan üstte. */
export function CentralStockSection() {
  const { t } = useTranslation();
  const canView = usePermission('CENTRAL_STOCK_VIEW');
  const query = useCentralStockTotals(canView);
  if (!canView) return null;
  return (
    <SectionCard
      title={t('report.charts.stock.title')}
      description={t('report.charts.stock.description')}
      icon="lots"
      testId="section-central-stock"
    >
      <QueryState query={query}>
        {(stock) => {
          const entries = stock.rows.map((row) => ({
            key: row.productId ?? row.yarnType,
            label: row.yarnType,
            kg: row.stockKg,
            note: row.stockKg.startsWith('-') ? t('report.dashboard.negativeStock') : undefined,
          }));
          if (entries.length === 0) {
            return <Text c="dimmed">{t('report.charts.stock.empty')}</Text>;
          }
          const other = (count: number) => t('report.charts.stock.other', { count });
          return (
            <BarList
              label={t('report.charts.stock.title')}
              testId="chart-central-stock"
              items={toKgBars(foldOthers(entries, other), formatKilograms)}
            />
          );
        }}
      </QueryState>
    </SectionCard>
  );
}
