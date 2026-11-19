import type { FireInvoiceRow, ListFireInvoicesParams } from '@iplik/api-client/settlements';
import {
  FIRE_INVOICES_PATH,
  FIRE_SETTLEMENTS_PATH,
  listFireInvoices,
  listFireSettlements,
} from '@iplik/api-client/settlements';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export const TO_INVOICE_PAGE_SIZE = 20;
export const INVOICED_PAGE_SIZE = 20;
/** Sunucunun izin verdiği en büyük sayfa; Excel için tüm sayfalar bu boyla okunur. */
const EXPORT_PAGE_SIZE = 100;

/** `page` 1 tabanlıdır (arayüz); sunucu 0 tabanlıdır. */
export function useToInvoice(page: number) {
  return useQuery({
    queryKey: [FIRE_SETTLEMENTS_PATH, 'TO_INVOICE', page],
    queryFn: () =>
      listFireSettlements({ status: 'TO_INVOICE', page: page - 1, size: TO_INVOICE_PAGE_SIZE }),
  });
}

/** Faturalananlar süzgeci: boş tarih ya da fasoncu "süzme" demektir. */
export type InvoicedFilter = { from: string; to: string; subcontractorId: string | undefined };

const filterParams = (filter: InvoicedFilter): ListFireInvoicesParams => ({
  ...(filter.from === '' ? {} : { from: filter.from }),
  ...(filter.to === '' ? {} : { to: filter.to }),
  ...(filter.subcontractorId === undefined ? {} : { subcontractorId: filter.subcontractorId }),
});

/** Girilmiş faturalar, yeniden eskiye; `page` 1 tabanlıdır. Başlangıç bitişten sonraysa sorgu atılmaz. */
export function useInvoiced(filter: InvoicedFilter, page: number, isEnabled: boolean) {
  return useQuery({
    queryKey: [FIRE_INVOICES_PATH, filter, page],
    queryFn: () =>
      listFireInvoices({ ...filterParams(filter), page: page - 1, size: INVOICED_PAGE_SIZE }),
    enabled: isEnabled,
    placeholderData: keepPreviousData,
  });
}

/** Süzgece uyan tüm faturalar (Excel için); sayfa sayfa okunur. */
export async function fetchAllInvoiced(filter: InvoicedFilter): Promise<FireInvoiceRow[]> {
  const rows: FireInvoiceRow[] = [];
  for (let page = 0; ; page += 1) {
    const result = await listFireInvoices({
      ...filterParams(filter),
      page,
      size: EXPORT_PAGE_SIZE,
    });
    rows.push(...result.items);
    if (page + 1 >= result.page.totalPages) return rows;
  }
}
