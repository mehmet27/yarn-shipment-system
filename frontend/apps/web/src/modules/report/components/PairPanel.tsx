import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { shortId } from '../format';
import { usePairReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { PartyTable } from './PartyTable';

/** Şoför–fasoncu eşleşmesi rapor sekmesi. */
export function PairPanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = usePairReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <PartyTable
          partyHeaders={[t('report.parties.driver'), t('report.parties.subcontractor')]}
          rows={rows.map((row) => ({
            ...row,
            key: `${row.driverId}:${row.subcontractorId}`,
            parties: [shortId(row.driverId), shortId(row.subcontractorId)],
          }))}
        />
      )}
    </QueryState>
  );
}
