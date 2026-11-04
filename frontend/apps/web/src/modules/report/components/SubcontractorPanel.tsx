import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { filterRows } from '../export/reportModel';
import { shortId } from '../format';
import { useSubcontractorReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { PartyTable } from './PartyTable';

/** Fasoncu bazında rapor sekmesi. */
export function SubcontractorPanel({
  params,
  isEnabled,
  isOnlyDiscrepant = false,
}: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useSubcontractorReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <PartyTable
          partyHeaders={[t('report.parties.subcontractor')]}
          rows={filterRows(rows, isOnlyDiscrepant).map((row) => ({
            ...row,
            key: row.id,
            parties: [shortId(row.id)],
          }))}
        />
      )}
    </QueryState>
  );
}
