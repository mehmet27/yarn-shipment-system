import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { filterRows } from '../export/reportModel';
import { usePartyNames } from '../hooks/usePartyNames';
import { usePairReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { PartyTable } from './PartyTable';

/** Şoför–fasoncu eşleşmesi rapor sekmesi. */
export function PairPanel({ params, isEnabled, isOnlyDiscrepant = false }: ReportPanelProps) {
  const { t } = useTranslation();
  const { nameOf } = usePartyNames();
  const query = usePairReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <PartyTable
          partyHeaders={[t('report.parties.driver'), t('report.parties.subcontractor')]}
          rows={filterRows(rows, isOnlyDiscrepant).map((row) => ({
            ...row,
            key: `${row.driverId}:${row.subcontractorId}`,
            parties: [nameOf(row.driverId), nameOf(row.subcontractorId)],
          }))}
        />
      )}
    </QueryState>
  );
}
