import type { GetDashboardParams } from '@iplik/api-client';
import {
  FIRE_INVOICES_PATH,
  FIRE_SETTLEMENTS_PATH,
  listFireInvoices,
  listFireSettlements,
} from '@iplik/api-client/settlements';
import { sumKg } from '@iplik/units';
import { useQuery } from '@tanstack/react-query';

/** Sunucunun izin verdiği en büyük sayfa. */
const PAGE_SIZE = 100;

/** Faturası bekleyen bütün dosyaların kalan kilosu; Faturalama > Faturalanacaklar listesinin toplamıdır. */
async function fetchToInvoiceKg(): Promise<string> {
  const remaining: string[] = [];
  for (let page = 0; ; page += 1) {
    const result = await listFireSettlements({ status: 'TO_INVOICE', page, size: PAGE_SIZE });
    remaining.push(...result.items.map((row) => row.remainingKg));
    if (page + 1 >= result.page.totalPages) return sumKg(remaining);
  }
}

/** Faturalanmayı bekleyen toplam kilo (dönemden bağımsız); yalnızca bakiye yetkisi olana sorulur. */
export function useToInvoiceKg(isEnabled: boolean) {
  return useQuery({
    queryKey: [FIRE_SETTLEMENTS_PATH, 'TO_INVOICE', 'total'],
    queryFn: fetchToInvoiceKg,
    enabled: isEnabled,
  });
}

/** Seçilen dönemde girilen faturaların toplam kilosu; Faturalama > Faturalananlar ile aynı süzgeç ve toplamdır. */
export function useInvoicedKg(params: GetDashboardParams, isEnabled: boolean) {
  const range = {
    ...(params.from === undefined ? {} : { from: params.from }),
    ...(params.to === undefined ? {} : { to: params.to }),
  };
  return useQuery({
    queryKey: [FIRE_INVOICES_PATH, 'total', range],
    queryFn: async () => (await listFireInvoices({ ...range, page: 0, size: 1 })).totalKg,
    enabled: isEnabled,
  });
}
