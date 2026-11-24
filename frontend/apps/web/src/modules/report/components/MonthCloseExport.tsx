import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, Select, Text } from '@iplik/ui';
import { useState } from 'react';
import { useMonthClose } from '../hooks/useMonthClose';
import { monthName, recentMonths } from '../monthClose';

/**
 * Ay sonu kapanışı: ay seçilir, tek PDF iner. Varsayılan, biten son aydır (kapanış çoğunlukla ertesi ayın ilk
 * günlerinde alınır).
 */
export function MonthCloseExport({ logo }: { logo: string | null }) {
  const { t } = useTranslation();
  const months = recentMonths();
  const [month, setMonth] = useState(months[1] ?? months[0] ?? '');
  const { isBusy, hasFailed, run } = useMonthClose(logo);
  return (
    <>
      <Group gap="sm" align="flex-end">
        <Select
          label={t('report.monthClose.month')}
          data={months.map((value) => ({ value, label: monthName(value) }))}
          value={month}
          onChange={(next) => {
            if (next !== null) setMonth(next);
          }}
          allowDeselect={false}
        />
        <Button
          variant="default"
          leftSection={<Icon name="pdf" />}
          loading={isBusy}
          onClick={() => void run(month)}
        >
          {t('report.monthClose.download')}
        </Button>
        <Text size="sm" c="dimmed" pb={8}>
          {t('report.monthClose.hint')}
        </Text>
      </Group>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('report.export.failed')}
        </Alert>
      )}
    </>
  );
}
