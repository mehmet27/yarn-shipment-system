import { useTranslation } from '@iplik/i18n';
import { Group, Stack, Text, Title } from '@iplik/ui';
import { useState } from 'react';
import { LogoControl } from '../components/LogoControl';
import { loadLogo, saveLogo } from '../logo';
import { DateRangeInputs } from '../components/DateRangeInputs';
import { ExportToolbar } from '../components/ExportToolbar';
import { PeriodSelect } from '../components/PeriodSelect';
import { ReportTabs } from '../components/ReportTabs';
import { useReportPeriod } from '../hooks/useReportPeriod';

/** Özet, şoför, fasoncu ve eşleşme raporları; Excel ve PDF çıktısı (ön analiz Bölüm 13). */
export function ReportsPage() {
  const { t } = useTranslation();
  const { days, setDays, range, setRange, hasRangeError, params } = useReportPeriod();
  const [isOnlyDiscrepant, setIsOnlyDiscrepant] = useState(false);
  const [logo, setLogo] = useState<string | null>(loadLogo);
  return (
    <Stack>
      <Group justify="space-between" align="flex-start">
        <Title order={1} size="h2">
          {t('report.title')}
        </Title>
        <Group align="flex-start">
          <PeriodSelect value={days} onChange={setDays} />
          <DateRangeInputs value={range} onChange={setRange} hasError={hasRangeError} />
        </Group>
      </Group>
      <Text size="sm" c="dimmed">
        {t('report.intro')}
      </Text>
      <LogoControl
        logo={logo}
        onChange={(next) => {
          setLogo(next);
          saveLogo(next);
        }}
      />
      <ExportToolbar
        params={params}
        isOnlyDiscrepant={isOnlyDiscrepant}
        onIsOnlyDiscrepantChange={setIsOnlyDiscrepant}
        isDisabled={hasRangeError}
        logo={logo}
      />
      <ReportTabs params={params} isOnlyDiscrepant={isOnlyDiscrepant} />
    </Stack>
  );
}
