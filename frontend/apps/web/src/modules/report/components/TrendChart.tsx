import type { MonthlyFlowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { ColumnChart } from '@iplik/ui/charts';
import { formatKg3 } from '../format';
import {
  toTrendRows,
  TREND_FABRIC_KEY,
  TREND_MONTH_KEY,
  TREND_SENT_KEY,
} from '../charts/trendRows';

const tick = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 0 });

/** Aylık giden iplik ve gelen kumaş sütunları; grafik kütüphanesi yalnızca bu bileşenle birlikte yüklenir. */
export function TrendChart({ flows }: { flows: readonly MonthlyFlowView[] }) {
  const { t } = useTranslation();
  return (
    <ColumnChart
      label={t('report.charts.trend.chartLabel')}
      testId="chart-trend"
      rows={toTrendRows(flows)}
      categoryKey={TREND_MONTH_KEY}
      series={[
        { key: TREND_SENT_KEY, label: t('report.charts.trend.sent'), color: 'blue' },
        { key: TREND_FABRIC_KEY, label: t('report.charts.trend.fabric'), color: 'orange' },
      ]}
      formatValue={(value) => formatKg3(value)}
      formatTick={(value) => tick.format(value)}
    />
  );
}
