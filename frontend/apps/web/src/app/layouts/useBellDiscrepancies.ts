import { DiscrepancyStatus, DISCREPANCIES_PATH, listDiscrepancies } from '@iplik/api-client';
import { useQueries } from '@tanstack/react-query';

const POLL_MS = 60_000;
const PREVIEW_SIZE = 5;

/**
 * Zilde görünecek karar bekleyen uyuşmazlıklar: yeni açılan (açık) ve kalitenin üstlenip incelediği (incelemede)
 * itirazlar; ikisi de müdürün kararını bekler. Liste ucu tek durum süzdüğünden iki sorgu birleştirilir.
 */
export function useBellDiscrepancies(isEnabled: boolean) {
  const results = useQueries({
    queries: [DiscrepancyStatus.OPEN, DiscrepancyStatus.IN_REVIEW].map((status) => ({
      queryKey: [DISCREPANCIES_PATH, 'bell', status],
      queryFn: () => listDiscrepancies({ status, size: PREVIEW_SIZE }),
      enabled: isEnabled,
      refetchInterval: POLL_MS,
    })),
  });
  return {
    total: results.reduce((sum, result) => sum + (result.data?.page.totalElements ?? 0), 0),
    items: results.flatMap((result) => result.data?.items ?? []).slice(0, PREVIEW_SIZE),
  };
}
