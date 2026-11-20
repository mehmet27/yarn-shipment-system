import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Button, Group, PageHero, Stack, Tabs, Text } from '@iplik/ui';
import { useState } from 'react';
import { usePermission } from '@/modules/auth';
import { InlineExportButtons } from '@/modules/report';
import { QueryState } from '@/modules/shared';
import { ProductionForm } from '../components/ProductionForm';
import { StockTable } from '../components/StockTable';
import { YarnReceiptForm } from '../components/YarnReceiptForm';
import { useCentralStock } from '../hooks/useCentralStock';
import { stockReportTables } from '../stockReport';

/** Merkez iplik stoku (CENTRAL_STOCK_VIEW): eldeki iplik sekmesi; yetkiliye iplik girişi ve kumaş girişi sekmeleri. */
export function CentralStockPage() {
  const { t } = useTranslation();
  const query = useCentralStock();
  const canReceipt = usePermission('CENTRAL_STOCK_RECORD');
  const canProduction = usePermission('CENTRAL_PRODUCTION_RECORD');
  const [tab, setTab] = useState<string | null>('stock');
  return (
    <Stack>
      <PageHero
        title={t('stock.title')}
        subtitle={t('stock.subtitle')}
        actions={
          <Group gap="sm">
            {query.dataUpdatedAt > 0 && (
              <Text size="xs" c="dimmed">
                {t('stock.updatedAt', {
                  at: formatDateTime(new Date(query.dataUpdatedAt).toISOString()),
                })}
              </Text>
            )}
            <Button
              variant="default"
              loading={query.isFetching}
              onClick={() => void query.refetch()}
            >
              {t('stock.refresh')}
            </Button>
          </Group>
        }
      />
      <Tabs value={tab} onChange={setTab} keepMounted={false}>
        <Tabs.List style={{ flexWrap: 'wrap', rowGap: 4 }}>
          <Tabs.Tab value="stock">{t('stock.tabs.stock')}</Tabs.Tab>
          {canReceipt && <Tabs.Tab value="receipt">{t('stock.tabs.receipt')}</Tabs.Tab>}
          {canProduction && <Tabs.Tab value="production">{t('stock.tabs.production')}</Tabs.Tab>}
          {tab === 'stock' && query.data !== undefined && (
            <InlineExportButtons
              tables={stockReportTables(query.data, t)}
              fileName="merkez-iplik-stoku.xlsx"
            />
          )}
        </Tabs.List>
        <Tabs.Panel value="stock" pt="md">
          <QueryState query={query}>{(stock) => <StockTable stock={stock} />}</QueryState>
        </Tabs.Panel>
        {canReceipt && (
          <Tabs.Panel value="receipt" pt="md">
            <YarnReceiptForm />
          </Tabs.Panel>
        )}
        {canProduction && (
          <Tabs.Panel value="production" pt="md">
            <ProductionForm />
          </Tabs.Panel>
        )}
      </Tabs>
    </Stack>
  );
}
