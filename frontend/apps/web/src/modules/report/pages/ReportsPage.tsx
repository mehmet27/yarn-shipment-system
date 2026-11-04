import { useTranslation } from '@iplik/i18n';
import { Group, Stack, Tabs, Text, Title } from '@iplik/ui';
import { useState } from 'react';
import { PeriodSelect } from '../components/PeriodSelect';
import { DriverPanel } from '../components/DriverPanel';
import { PairPanel } from '../components/PairPanel';
import { SubcontractorPanel } from '../components/SubcontractorPanel';
import type { ReportKind } from '../hooks/usePartyReport';
import { REPORT_KINDS } from '../hooks/usePartyReport';
import type { PeriodDays } from '../period';
import { DEFAULT_PERIOD, toPeriodParams } from '../period';

/** Şoför, fasoncu ve eşleşme bazlı göstergeler (ön analiz Bölüm 13). */
export function ReportsPage() {
  const { t } = useTranslation();
  const [days, setDays] = useState<PeriodDays>(DEFAULT_PERIOD);
  const [kind, setKind] = useState<ReportKind>('drivers');
  const params = toPeriodParams(days);
  return (
    <Stack>
      <Group justify="space-between">
        <Title order={1} size="h2">
          {t('report.title')}
        </Title>
        <PeriodSelect value={days} onChange={setDays} />
      </Group>
      <Text size="sm" c="dimmed">
        {t('report.intro')}
      </Text>
      <Tabs
        value={kind}
        onChange={(next) => {
          const known = REPORT_KINDS.find((item) => item === next);
          if (known !== undefined) setKind(known);
        }}
      >
        <Tabs.List>
          {REPORT_KINDS.map((item) => (
            <Tabs.Tab key={item} value={item}>
              {t(`report.tabs.${item}`)}
            </Tabs.Tab>
          ))}
        </Tabs.List>
        <Tabs.Panel value="drivers" pt="md">
          <DriverPanel params={params} isEnabled={kind === 'drivers'} />
        </Tabs.Panel>
        <Tabs.Panel value="subcontractors" pt="md">
          <SubcontractorPanel params={params} isEnabled={kind === 'subcontractors'} />
        </Tabs.Panel>
        <Tabs.Panel value="pairs" pt="md">
          <PairPanel params={params} isEnabled={kind === 'pairs'} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  );
}
