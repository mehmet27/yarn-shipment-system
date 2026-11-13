import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Button, Group, Stack, Tabs, Text, Title } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { ProductionForm } from '../components/ProductionForm';
import { StockTable } from '../components/StockTable';
import { YarnReceiptForm } from '../components/YarnReceiptForm';
import { useCentralStock } from '../hooks/useCentralStock';

/** Merkez iplik stoku (CENTRAL_STOCK_VIEW): eldeki iplik sekmesi; yetkiliye iplik girişi ve kumaş girişi sekmeleri. */
export function CentralStockPage() {
  const { t } = useTranslation();
  const query = useCentralStock();
  const canReceipt = usePermission('CENTRAL_STOCK_RECORD');
  const canProduction = usePermission('CENTRAL_PRODUCTION_RECORD');
  return (
    <Stack>
      <Group justify="space-between" align="flex-start">
        <Stack gap={0}>
          <Title order={1}>{t('stock.title')}</Title>
          <Text c="dimmed">{t('stock.subtitle')}</Text>
        </Stack>
        <Stack gap={2} align="flex-end">
          <Button variant="default" loading={query.isFetching} onClick={() => void query.refetch()}>
            {t('stock.refresh')}
          </Button>
          {query.dataUpdatedAt > 0 && (
            <Text size="xs" c="dimmed">
              {t('stock.updatedAt', {
                at: formatDateTime(new Date(query.dataUpdatedAt).toISOString()),
              })}
            </Text>
          )}
        </Stack>
      </Group>
      <Tabs defaultValue="stock" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="stock">{t('stock.tabs.stock')}</Tabs.Tab>
          {canReceipt && <Tabs.Tab value="receipt">{t('stock.tabs.receipt')}</Tabs.Tab>}
          {canProduction && <Tabs.Tab value="production">{t('stock.tabs.production')}</Tabs.Tab>}
        </Tabs.List>
        <Tabs.Panel value="stock" pt="md">
          <QueryState query={query}>{(stock) => <StockTable stock={stock} />}</QueryState>
        </Tabs.Panel>
        {canReceipt && (
          <Tabs.Panel value="receipt" pt="md">
            <YarnReceiptForm yarnTypes={query.data?.rows.map((row) => row.yarnType) ?? []} />
          </Tabs.Panel>
        )}
        {canProduction && (
          <Tabs.Panel value="production" pt="md">
            <ProductionForm yarnTypes={query.data?.rows.map((row) => row.yarnType) ?? []} />
          </Tabs.Panel>
        )}
      </Tabs>
    </Stack>
  );
}
