import type { YarnVsFabricRowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { BarList } from '@iplik/ui';
import { toFireBars } from '../charts/fireBars';

/** Fasoncu başına gerçekleşen fire oranı çubukları; rakamlar "Giden iplik ↔ gelen kumaş" tablosuyla aynıdır. */
type FireBarsProps = { rows: readonly YarnVsFabricRowView[]; testId: string };

export function FireBars({ rows, testId }: FireBarsProps) {
  const { t } = useTranslation();
  return (
    <BarList
      label={t('report.charts.fire.title')}
      testId={testId}
      items={toFireBars(rows, (key, options) => t(key, options ?? {}))}
    />
  );
}
