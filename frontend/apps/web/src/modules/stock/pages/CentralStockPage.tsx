import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title } from '@iplik/ui';
import { Can } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { ProductionForm } from '../components/ProductionForm';
import { StockTable } from '../components/StockTable';
import { YarnReceiptForm } from '../components/YarnReceiptForm';
import { useCentralStock } from '../hooks/useCentralStock';

/** Merkez iplik stoku (CENTRAL_STOCK_VIEW): eldeki iplik, iplik girişi ve merkez fabrika kumaş girişi. */
export function CentralStockPage() {
  const { t } = useTranslation();
  const query = useCentralStock();
  return (
    <Stack>
      <Title order={1}>{t('stock.title')}</Title>
      <Text c="dimmed">{t('stock.subtitle')}</Text>
      <QueryState query={query}>{(stock) => <StockTable stock={stock} />}</QueryState>
      <Can permission="CENTRAL_STOCK_RECORD">
        <YarnReceiptForm />
      </Can>
      <Can permission="CENTRAL_PRODUCTION_RECORD">
        <ProductionForm />
      </Can>
    </Stack>
  );
}
