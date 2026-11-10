import { useTranslation } from '@iplik/i18n';
import { QueryState } from '@/modules/shipment';
import { useToInvoiceReport } from '../hooks/useFaconReports';
import { FireList } from './FireList';
import type { ReportPanelProps } from './reportPanelProps';

/** Hatalı kilosu olan ve faturalanmamış dosyalar. */
export function ToInvoicePanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useToInvoiceReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) => (
        <FireList
          rows={rows}
          title={t('report.export.sheets.toInvoice')}
          fileName="faturalanacaklar.xlsx"
          withStatus={false}
        />
      )}
    </QueryState>
  );
}
