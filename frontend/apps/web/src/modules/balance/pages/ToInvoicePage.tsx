import { useTranslation } from '@iplik/i18n';
import { Anchor, Box, Group, PageHero, Paper, Stack, Text } from '@iplik/ui';
import { formatKgText, formatPercentText, ListPagination, QueryState } from '@/modules/shipment';
import { useState } from 'react';
import { Link } from 'react-router';
import { InvoiceAction } from '../components/InvoiceAction';
import { useToInvoice } from '../hooks/useFireSettlements';

/** Faturalanacaklar: hatalı kilosu olan ve faturası bekleyen fire dosyaları (TO_INVOICE). */
export function ToInvoicePage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(1);
  const query = useToInvoice(page);
  return (
    <Stack>
      <PageHero
        title={t('settlement.toInvoice.title')}
        subtitle={t('settlement.toInvoice.subtitle')}
      />
      <QueryState query={query}>
        {({ items, page: info }) =>
          items.length === 0 ? (
            <Text c="dimmed">{t('settlement.toInvoice.empty')}</Text>
          ) : (
            <Stack>
              <Paper withBorder style={{ overflow: 'hidden' }}>
                {items.map((row, index) => (
                  <Box
                    key={row.shipmentId}
                    component="article"
                    aria-label={row.shipmentNo}
                    p="md"
                    style={{
                      borderTop: index === 0 ? undefined : '1px solid var(--app-line-soft)',
                    }}
                  >
                    <Stack gap={4}>
                      <Group justify="space-between" gap="xs">
                        <Anchor component={Link} to={`/panel/sevkiyat/${row.shipmentId}`} fw={600}>
                          {row.shipmentNo}
                        </Anchor>
                        <Text size="sm">{row.subcontractorTitle}</Text>
                      </Group>
                      <Text size="sm" c="dimmed">
                        {t('settlement.toInvoice.sent')} {formatKgText(row.sentKg)} ·{' '}
                        {t('settlement.toInvoice.fabric')} {formatKgText(row.fabricKg)} ·{' '}
                        {t('settlement.toInvoice.expected')} {formatKgText(row.expectedFabricKg)}
                      </Text>
                      <Text fw={600}>
                        {t('settlement.toInvoice.faulty')} {formatKgText(row.faultyKg)} ·{' '}
                        {t('settlement.toInvoice.fire')} {formatPercentText(row.actualFirePercent)}
                      </Text>
                      <InvoiceAction shipmentId={row.shipmentId} />
                    </Stack>
                  </Box>
                ))}
              </Paper>
              {info.totalPages > 1 && (
                <ListPagination total={info.totalPages} value={page} onChange={setPage} />
              )}
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
