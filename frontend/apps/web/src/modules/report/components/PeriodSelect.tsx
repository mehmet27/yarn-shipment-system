import { useTranslation } from '@iplik/i18n';
import { SegmentedControl } from '@iplik/ui';
import type { PeriodDays } from '../period';
import { PERIOD_DAYS } from '../period';

export type PeriodSelectProps = {
  value: PeriodDays;
  onChange: (value: PeriodDays) => void;
};

export function PeriodSelect({ value, onChange }: PeriodSelectProps) {
  const { t } = useTranslation();
  return (
    <SegmentedControl
      aria-label={t('report.period.label')}
      value={value}
      onChange={(next) => {
        const known = PERIOD_DAYS.find((days) => days === next);
        if (known !== undefined) onChange(known);
      }}
      data={PERIOD_DAYS.map((days) => ({ value: days, label: t(`report.period.days${days}`) }))}
    />
  );
}
