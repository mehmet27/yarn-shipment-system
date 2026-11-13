import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Alert, Stack, Title } from '@iplik/ui';
import { PanelExportBar } from '@/modules/report';
import { stockReportTables } from '../stockReport';
import { MovementRows } from './MovementRows';
import { StockRows } from './StockRows';
import { StockSummary } from './StockSummary';

/** Merkezde iplik cinsi başına eldeki kilo ve son hareketler; ekrandaki veri Excel/PDF olarak indirilir. */
export function StockTable({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const negative = stock.rows
    .filter((row) => row.stockKg.startsWith('-'))
    .map((row) => row.yarnType);
  return (
    <Stack>
      <StockSummary stock={stock} />
      {negative.length > 0 && (
        <Alert color="yellow">{`${t('stock.negativeWarning')} (${negative.join(', ')})`}</Alert>
      )}
      <PanelExportBar tables={stockReportTables(stock, t)} fileName="merkez-iplik-stoku.xlsx" />
      <StockRows stock={stock} />
      <Title order={2} size="h3">
        {t('stock.movements')}
      </Title>
      <MovementRows movements={stock.movements} />
    </Stack>
  );
}
