import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { ListPagination, QueryState } from '@/modules/shipment';
import { useState } from 'react';
import { useToInvoice } from '../hooks/useFireSettlements';
import { ToInvoiceRow } from './ToInvoiceRow';

/** Faturalanacaklar sekmesi: hatalı kilosu olan ve faturası bekleyen fire dosyaları (TO_INVOICE). */
export function ToInvoiceTab() {
  const { t } = useTranslation();
  const [page, setPage] = useState(1);
  const query = useToInvoice(page);
  return (
    <QueryState query={query}>
      {({ items, page: info }) =>
        items.length === 0 ? (
          <Text c="dimmed">{t('settlement.toInvoice.empty')}</Text>
        ) : (
          <Stack>
            <Paper withBorder style={{ overflow: 'hidden' }}>
              {items.map((row, index) => (
                <ToInvoiceRow key={row.shipmentId} row={row} isFirst={index === 0} />
              ))}
            </Paper>
            {info.totalPages > 1 && (
              <ListPagination total={info.totalPages} value={page} onChange={setPage} />
            )}
          </Stack>
        )
      }
    </QueryState>
  );
}
