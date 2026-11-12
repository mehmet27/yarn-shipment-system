import { CENTRAL_STOCK_PATH, getCentralStock } from '@iplik/api-client/central-stock';
import { BALANCES_PATH, getBalances } from '@iplik/api-client/settlements';
import { useQuery } from '@tanstack/react-query';

/** Merkez depodaki iplik ve merkezde üretilen kumaş; yalnızca merkez stok yetkisi olana sorulur. */
export function useCentralStockTotals(isEnabled: boolean) {
  return useQuery({
    queryKey: [CENTRAL_STOCK_PATH],
    queryFn: getCentralStock,
    enabled: isEnabled,
  });
}

/** Fasoncularda duran iplik ve yoldakiler; yalnızca bakiye yetkisi olana sorulur. */
export function useSubcontractorYarn(isEnabled: boolean) {
  return useQuery({
    queryKey: [BALANCES_PATH, 'matrix', undefined],
    queryFn: () => getBalances(undefined),
    enabled: isEnabled,
  });
}
