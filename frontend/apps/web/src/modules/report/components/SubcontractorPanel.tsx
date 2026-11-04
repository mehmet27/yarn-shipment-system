import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { shortId } from '../format';
import { useSubcontractorReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { PartyTable } from './PartyTable';

/** Fasoncu bazında rapor sekmesi. */
export function SubcontractorPanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useSubcontractorReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <PartyTable
          partyHeaders={[t('report.parties.subcontractor')]}
          rows={rows.map((row) => ({ ...row, key: row.id, parties: [shortId(row.id)] }))}
        />
      )}
    </QueryState>
  );
}
