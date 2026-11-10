import { useTranslation } from '@iplik/i18n';
import { Anchor, Paper, Stack, Text, Title } from '@iplik/ui';
import { formatKgText, formatPercentText, ListPagination, QueryState } from '@/modules/shipment';
import { useState } from 'react';
import { Link } from 'react-router';
import { useToInvoice } from '../hooks/useFireSettlements';

/** Faturalanacaklar: hatalı kilosu olan ve faturası bekleyen fire dosyaları (TO_INVOICE). */
export function ToInvoicePage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(1);
  const query = useToInvoice(page);
  return (
    <Stack>
      <Title order={1}>{t('settlement.toInvoice.title')}</Title>
      <Text c="dimmed">{t('settlement.toInvoice.subtitle')}</Text>
      <QueryState query={query}>
        {({ items, page: info }) =>
          items.length === 0 ? (
            <Text c="dimmed">{t('settlement.toInvoice.empty')}</Text>
          ) : (
            <Stack>
              {items.map((row) => (
                <Paper
                  key={row.shipmentId}
                  withBorder
                  p="md"
                  component="article"
                  aria-label={row.shipmentNo}
                >
                  <Stack gap="xs">
                    <Anchor component={Link} to={`/panel/sevkiyat/${row.shipmentId}`} fw={600}>
                      {row.shipmentNo}
                    </Anchor>
                    <Text size="sm">{row.subcontractorTitle}</Text>
                    <Text size="sm" c="dimmed">
                      {t('settlement.toInvoice.sent')} {formatKgText(row.sentKg)} ·{' '}
                      {t('settlement.toInvoice.fabric')} {formatKgText(row.fabricKg)} ·{' '}
                      {t('settlement.toInvoice.expected')} {formatKgText(row.expectedFabricKg)}
                    </Text>
                    <Text fw={600}>
                      {t('settlement.toInvoice.faulty')} {formatKgText(row.faultyKg)} ·{' '}
                      {t('settlement.toInvoice.fire')} {formatPercentText(row.actualFirePercent)}
                    </Text>
                  </Stack>
                </Paper>
              ))}
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
