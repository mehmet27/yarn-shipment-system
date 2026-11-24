import { useTranslation } from '@iplik/i18n';
import { Tabs } from '@iplik/ui';
import type { ReportKind } from '../hooks/usePartyReport';
import { REPORT_KINDS } from '../hooks/usePartyReport';
import { BalanceMatrixPanel } from './BalanceMatrixPanel';
import { BalancePanel } from './BalancePanel';
import { FirePanel } from './FirePanel';
import type { ReportPanelProps } from './reportPanelProps';
import { ShipmentsPanel } from './ShipmentsPanel';
import { SubcontractorPanel } from './SubcontractorPanel';
import { SummaryPanel } from './SummaryPanel';
import { ToInvoicePanel } from './ToInvoicePanel';
import { YarnVsFabricPanel } from './YarnVsFabricPanel';

export type ReportTabsProps = Pick<ReportPanelProps, 'params'> & {
  isOnlyDiscrepant: boolean;
  kind: ReportKind;
  onKindChange: (kind: ReportKind) => void;
};

/** Özet, fasoncu, sevkiyat dökümü ve bakiye sekmeleri; yalnızca açık sekme veri çeker. */
export function ReportTabs({ params, isOnlyDiscrepant, kind, onKindChange }: ReportTabsProps) {
  const { t } = useTranslation();
  return (
    <Tabs
      value={kind}
      onChange={(next) => {
        const known = REPORT_KINDS.find((item) => item === next);
        if (known !== undefined) onKindChange(known);
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
      <Tabs.Panel value="subcontractors" pt="md">
        <SubcontractorPanel
          params={params}
          isEnabled={kind === 'subcontractors'}
          isOnlyDiscrepant={isOnlyDiscrepant}
        />
      </Tabs.Panel>
      <Tabs.Panel value="shipments" pt="md">
        <ShipmentsPanel
          params={params}
          isEnabled={kind === 'shipments'}
          isOnlyDiscrepant={isOnlyDiscrepant}
        />
      </Tabs.Panel>
      <Tabs.Panel value="balance" pt="md">
        <BalancePanel params={params} isEnabled={kind === 'balance'} />
      </Tabs.Panel>
      <Tabs.Panel value="balanceMatrix" pt="md">
        <BalanceMatrixPanel params={params} isEnabled={kind === 'balanceMatrix'} />
      </Tabs.Panel>
      <Tabs.Panel value="fire" pt="md">
        <FirePanel params={params} isEnabled={kind === 'fire'} />
      </Tabs.Panel>
      <Tabs.Panel value="yarnVsFabric" pt="md">
        <YarnVsFabricPanel params={params} isEnabled={kind === 'yarnVsFabric'} />
      </Tabs.Panel>
      <Tabs.Panel value="toInvoice" pt="md">
        <ToInvoicePanel params={params} isEnabled={kind === 'toInvoice'} />
      </Tabs.Panel>
    </Tabs>
  );
}
