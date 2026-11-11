import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { useFireReport } from '../hooks/useFaconReports';
import { FireList } from './FireList';
import type { ReportPanelProps } from './reportPanelProps';

/** Sevkiyat bazında G, D, K, beklenen, gerçekleşen fire ve fatura durumu. */
export function FirePanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useFireReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <FireList
          rows={rows}
          title={t('report.export.sheets.fire')}
          fileName="fire-raporu.xlsx"
          withStatus
        />
      )}
    </QueryState>
  );
}
