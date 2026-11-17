import { CENTRAL_STOCK_PATH, getCentralStock } from '@iplik/api-client/central-stock';
import { useQuery } from '@tanstack/react-query';
import { calculateNetKg, isKilogram, sumKg } from '@iplik/units';
import { usePermission } from '@/modules/auth';

/** Reçetedeki bir iplik cinsi: listeden seçilmişse `productId` dolu, kilolar o cinse toplanır. */
export type YarnDemand = { productId: string | undefined; yarn: string; kg: string };

export type StockLine = { yarn: string; neededKg: string; stockKg: string; missingKg: string };

/**
 * Merkez stoğu yetkisi olan kullanıcı için reçetedeki iplikleri merkezdeki stokla karşılaştırır. Serbest metin
 * cinsler stokla eşlenemediği için kontrol dışı kalır.
 */
export function useCentralStockCheck(demands: readonly YarnDemand[]): readonly StockLine[] {
  const stock = useQuery({
    queryKey: [CENTRAL_STOCK_PATH],
    queryFn: getCentralStock,
    enabled: usePermission('CENTRAL_STOCK_VIEW'),
  }).data;
  if (stock === undefined) return [];
  const needed = new Map<string, { yarn: string; kgs: string[] }>();
  for (const demand of demands) {
    if (demand.productId === undefined || demand.kg.trim() === '') continue;
    const entry = needed.get(demand.productId) ?? { yarn: demand.yarn, kgs: [] };
    entry.kgs.push(demand.kg);
    needed.set(demand.productId, entry);
  }
  return [...needed].map(([productId, entry]) => {
    const found = stock.rows.find((row) => row.productId === productId)?.stockKg ?? '';
    const stockKg = isKilogram(found) ? found : '0.000';
    const neededKg = sumKg(entry.kgs);
    const missingKg =
      isKilogram(neededKg) && isKilogram(stockKg) ? calculateNetKg(neededKg, stockKg) : undefined;
    return { yarn: entry.yarn, neededKg, stockKg, missingKg: missingKg ?? '0.000' };
  });
}
