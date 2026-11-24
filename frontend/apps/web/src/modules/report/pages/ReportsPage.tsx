import { useTranslation } from '@iplik/i18n';
import { Group, PageHero, Paper, Stack } from '@iplik/ui';
import { useState } from 'react';
import { LogoControl } from '../components/LogoControl';
import { loadLogo, saveLogo } from '../logo';
import { DateRangeInputs } from '../components/DateRangeInputs';
import { ExportToolbar } from '../components/ExportToolbar';
import { PeriodSelect } from '../components/PeriodSelect';
import { ReportTabs } from '../components/ReportTabs';
import { useReportView } from '../hooks/useReportView';

/** Özet, şoför, fasoncu ve eşleşme raporları; Excel ve PDF çıktısı (ön analiz Bölüm 13). */
export function ReportsPage() {
  const { t } = useTranslation();
  const view = useReportView();
  const { days, setDays, range, setRange, hasRangeError, params } = view;
  const [logo, setLogo] = useState<string | null>(loadLogo);
  return (
    <Stack>
      <PageHero title={t('report.title')} subtitle={t('report.intro')} />
      <Paper withBorder p="md">
        <Stack gap="sm">
          <Group align="flex-start" gap="sm">
            <PeriodSelect value={days} onChange={setDays} />
            <DateRangeInputs value={range} onChange={setRange} hasError={hasRangeError} />
          </Group>
          <ExportToolbar
            params={params}
            isOnlyDiscrepant={view.isOnlyDiscrepant}
            onIsOnlyDiscrepantChange={view.setIsOnlyDiscrepant}
            isDisabled={hasRangeError}
            logo={logo}
          >
            <LogoControl
              logo={logo}
              onChange={(next) => {
                setLogo(next);
                saveLogo(next);
              }}
            />
          </ExportToolbar>
        </Stack>
      </Paper>
      <ReportTabs
        params={params}
        isOnlyDiscrepant={view.isOnlyDiscrepant}
        kind={view.kind}
        onKindChange={view.setKind}
      />
    </Stack>
  );
}
