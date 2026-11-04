import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { filterRows } from '../export/reportModel';
import { shortId } from '../format';
import { useDriverReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { PartyTable } from './PartyTable';

/** Şoför bazında rapor sekmesi. */
export function DriverPanel({ params, isEnabled, isOnlyDiscrepant = false }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useDriverReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <PartyTable
          partyHeaders={[t('report.parties.driver')]}
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
