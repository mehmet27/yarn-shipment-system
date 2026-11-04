import { useTranslation } from '@iplik/i18n';
import { Tabs } from '@iplik/ui';
import { useState } from 'react';
import type { ReportKind } from '../hooks/usePartyReport';
import { REPORT_KINDS } from '../hooks/usePartyReport';
import { DriverPanel } from './DriverPanel';
import { PairPanel } from './PairPanel';
import type { ReportPanelProps } from './reportPanelProps';
import { SubcontractorPanel } from './SubcontractorPanel';
import { SummaryPanel } from './SummaryPanel';

type ReportTabsProps = Pick<ReportPanelProps, 'params'> & { isOnlyDiscrepant: boolean };

/** Özet, şoför, fasoncu ve eşleşme sekmeleri; yalnızca açık sekme veri çeker. */
export function ReportTabs({ params, isOnlyDiscrepant }: ReportTabsProps) {
  const { t } = useTranslation();
  const [kind, setKind] = useState<ReportKind>('summary');
  return (
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
      <Tabs.Panel value="summary" pt="md">
        <SummaryPanel params={params} isEnabled={kind === 'summary'} />
      </Tabs.Panel>
      <Tabs.Panel value="drivers" pt="md">
        <DriverPanel
          params={params}
          isEnabled={kind === 'drivers'}
          isOnlyDiscrepant={isOnlyDiscrepant}
        />
      </Tabs.Panel>
      <Tabs.Panel value="subcontractors" pt="md">
        <SubcontractorPanel
          params={params}
          isEnabled={kind === 'subcontractors'}
          isOnlyDiscrepant={isOnlyDiscrepant}
        />
      </Tabs.Panel>
      <Tabs.Panel value="pairs" pt="md">
        <PairPanel
          params={params}
          isEnabled={kind === 'pairs'}
          isOnlyDiscrepant={isOnlyDiscrepant}
        />
      </Tabs.Panel>
    </Tabs>
  );
}
