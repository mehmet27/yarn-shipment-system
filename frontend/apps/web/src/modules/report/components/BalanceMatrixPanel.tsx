import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { balanceMatrixTable } from '../export/faconTables';
import { useBalanceMatrix } from '../hooks/useFaconReports';
import { pivotBalanceMatrix } from '../pivot';
import { MatrixTable } from './MatrixTable';
import { OpeningImportForm } from './OpeningImportForm';
import { PanelExportBar } from './PanelExportBar';
import type { ReportPanelProps } from './reportPanelProps';

/** İplik cinsi × fasoncu bakiye matrisi; yolda kalan ayrı sütundadır. */
export function BalanceMatrixPanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useBalanceMatrix(params, isEnabled);
  return (
    <Stack>
      <QueryState query={query}>
        {(cells) => {
          const matrix = pivotBalanceMatrix(cells);
          return (
            <Stack>
              <PanelExportBar
                tables={[balanceMatrixTable(matrix, t)]}
                fileName="bakiye-matrisi.xlsx"
              />
              {matrix.rows.length === 0 ? (
                <Text c="dimmed">{t('report.matrix.empty')}</Text>
              ) : (
                <MatrixTable matrix={matrix} />
              )}
            </Stack>
          );
        }}
      </QueryState>
      <OpeningImportForm />
    </Stack>
  );
}
