import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Box, SegmentedBar } from '@iplik/ui';

/** Dönemdeki sevkiyatların duruma göre dağılımı; sayılar altındaki hücrelerle aynıdır. */
export function ShipmentStatusBar({ shipments }: { shipments: DashboardView['shipments'] }) {
  const { t } = useTranslation();
  const parts = [
    { key: 'inProgress', label: t('report.dashboard.inProgress'), color: 'blue' },
    { key: 'inReview', label: t('report.dashboard.inReview'), color: 'orange' },
    { key: 'completed', label: t('report.dashboard.completed'), color: 'green' },
    { key: 'cancelled', label: t('report.export.cancelled'), color: 'gray' },
  ] as const;
  return (
    <Box p="md" style={{ borderBottom: '1px solid var(--app-line)' }}>
      <SegmentedBar
        label={t('report.charts.status.label')}
        testId="chart-shipment-status"
        parts={parts.map((part) => ({
          ...part,
          value: shipments[part.key],
          valueLabel: String(shipments[part.key]),
        }))}
      />
    </Box>
  );
}
